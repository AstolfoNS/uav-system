from typing import Dict, Any
from fastapi import APIRouter, Request, FastAPI
from fastapi.staticfiles import StaticFiles
from starlette.templating import Jinja2Templates

from yoloservice.config.settings import settings
from yoloservice.web.router import autodiscover, build_routers


router = APIRouter(tags=["System"])
template = Jinja2Templates(directory=str(settings.PROJECT_DIR / "resource" / "template"))


class RouteVisualizer:
    @staticmethod
    def build_tree(routes) -> Dict[str, Any]:
        # 初始树只包含一个代表 "/" 的根节点
        tree = {"/": {}}

        for route in routes:
            if any(x in route.path for x in ["/openapi.json", "/docs", "/redoc", "/favicon.ico"]):
                continue

            # 如果就是根路由本身，直接把信息挂在 "/" 节点下
            if route.path == "/":
                current = tree["/"]
            else:
                # 其他路径如 /api/v1，作为 "/" 的子孙
                parts = [p for p in route.path.split("/") if p]
                current = tree["/"]
                for part in parts:
                    current = current.setdefault(part, {})

            # 写入方法和模块信息
            methods = getattr(route, "methods", ["GET"])
            existing_methods = current.setdefault("_methods", [])
            for m in methods:
                if m not in ("HEAD", "OPTIONS") and m not in existing_methods:
                    existing_methods.append(m)
            
            endpoint = getattr(route, "endpoint", None)
            module = getattr(endpoint, "__module__", "static") if endpoint else "static"
            current["_module"] = module.split(".")[-1]
        return tree

    @staticmethod
    def render_html_tree(tree: Dict[str, Any]) -> str:
        html = ""
        keys = [k for k in tree.keys() if not k.startswith("_")]
        keys.sort(key=lambda x: (len([sub for sub in tree[x].keys() if not sub.startswith("_")]) == 0, x.startswith("{"), x))

        for key in keys:
            subtree = tree[key]
            sub_keys = [k for k in subtree.keys() if not k.startswith("_")]

            methods = subtree.get("_methods", [])
            method_list = []
            for m in methods:
                cls_name = f"badge-{m.lower()}" if m in ["GET", "POST", "PUT", "DELETE", "PATCH"] else "badge-other"
                method_list.append(f'<span class="badge {cls_name}">{m}</span>')
            method_html = "".join(method_list)

            module_info = f'<span class="module">@{subtree.get("_module", "")}</span>' if subtree.get("_module") else ""

            # 样式区分
            if key == "/":
                style_class = "root-node"
                display_name = "/"
            else:
                style_class = "param" if "{" in key else "folder"
                display_name = key

            if sub_keys:
                # 递归渲染，不再需要特殊的 is_root 判断，CSS 会自动处理缩进
                html += f"""
                <details class="tree-node" open>
                    <summary>
                        <span class="icon"></span><span class="{style_class}">{display_name}</span>{method_html}{module_info}
                    </summary>
                    <div class="subtree">
                        {RouteVisualizer.render_html_tree(subtree)}
                    </div>
                </details>
                """
            else:
                html += f"""
                <div class="tree-leaf">
                    <span class="icon-leaf"></span><span class="{style_class}">{display_name}</span>{method_html}{module_info}
                </div>
                """
        return html


@router.get("/", include_in_schema=False)
async def root_view(request: Request):
    try:
        app = request.app
        tree = RouteVisualizer.build_tree(app.routes)

        # 这里直接获取生成的完整 HTML 字符串
        html_content = RouteVisualizer.render_html_tree(tree)

        return template.TemplateResponse(
            request=request,
            name="route_explorer.html",
            context={
                "request": request,
                "project_name": settings.PROJECT_NAME,
                "env": "DEBUG" if settings.DEBUG else "PROD",
                "version": settings.PROJECT_VERSION,
                "tree_lines": html_content  # 传入字符串
            }
        )
    except Exception as e:
        # 这会让错误详情直接显示在浏览器里，方便调试
        print(f"Error {e}")

        from fastapi.responses import PlainTextResponse
        import traceback
        return PlainTextResponse(traceback.format_exc(), status_code=500)


def setup_routes(app: FastAPI):
    import os
    # 确保输出目录优先存在，防止 StaticFiles 挂载时报错
    os.makedirs(settings.OUTPUT_DIR, exist_ok=True)
    
    app.mount(settings.STATIC_URL, StaticFiles(directory=settings.OUTPUT_DIR), name="static")
    app.include_router(router)
    autodiscover()
    for r in build_routers():
        app.include_router(r, prefix=settings.API_PREFIX)
