import {
  createRouter,
  createWebHistory,
  type RouteRecordRaw,
} from "vue-router";
import { hasAccessToken } from "@/composables/useAuth";
import { fetchMyProfile } from "@/api";

const MainLayout = () => import("@/layouts/MainLayout.vue");
const LoginView = () => import("@/views/LoginView.vue");
const DashboardView = () => import("@/views/DashboardView.vue");
const UserProfileView = () => import("@/views/UserProfileView.vue");
const NodeManagementView = () => import("@/views/NodeManagementView.vue");
const InferenceView = () => import("@/views/InferenceView.vue");
const RecordsView = () => import("@/views/RecordsView.vue");
const RbacAdminView = () => import("@/views/RbacAdminView.vue");
const NotFoundView = () => import("@/views/NotFoundView.vue");

const routes: RouteRecordRaw[] = [
  {
    path: "/login",
    name: "login",
    component: LoginView,
    meta: { public: true },
  },
  {
    path: "/",
    component: MainLayout,
    children: [
      {
        path: "",
        redirect: "/dashboard",
      },
      {
        path: "dashboard",
        name: "dashboard",
        component: DashboardView,
      },
      {
        path: "profile",
        name: "profile",
        component: UserProfileView,
      },
      {
        path: "nodes",
        name: "nodes",
        component: NodeManagementView,
      },
      {
        path: "inference",
        name: "inference",
        component: InferenceView,
        meta: { keepAlive: true },
      },
      {
        path: "records",
        name: "records",
        component: RecordsView,
      },
      {
        path: "rbac",
        name: "rbac",
        component: RbacAdminView,
        meta: { requiresAdmin: true },
      },
    ],
  },
  {
    path: "/:pathMatch(.*)*",
    name: "not-found",
    component: NotFoundView,
    meta: { public: true },
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach(async (to) => {
  if (to.meta.public) {
    if (to.path === "/login" && hasAccessToken()) {
      return "/dashboard";
    }
    return true;
  }

  if (!hasAccessToken()) {
    return "/login";
  }

  if (to.meta.requiresAdmin) {
    try {
      const profile = await fetchMyProfile();
      const roles = Array.isArray(profile.roles)
        ? profile.roles.map((item) => String(item).toLowerCase())
        : [];
      if (!roles.includes("admin")) {
        return "/dashboard";
      }
    } catch {
      return "/dashboard";
    }
  }

  return true;
});

export default router;
