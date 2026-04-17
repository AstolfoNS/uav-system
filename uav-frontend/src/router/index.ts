import {
  createRouter,
  createWebHistory,
  type RouteRecordRaw,
} from "vue-router";
import { hasAccessToken } from "@/composables/useAuth";

const MainLayout = () => import("@/layouts/MainLayout.vue");
const LoginView = () => import("@/views/LoginView.vue");
const DashboardView = () => import("@/views/DashboardView.vue");
const UserProfileView = () => import("@/views/UserProfileView.vue");
const NodeManagementView = () => import("@/views/NodeManagementView.vue");
const InferenceView = () => import("@/views/InferenceView.vue");
const RecordsView = () => import("@/views/RecordsView.vue");
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
      },
      {
        path: "records",
        name: "records",
        component: RecordsView,
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

router.beforeEach((to) => {
  if (to.meta.public) {
    if (to.path === "/login" && hasAccessToken()) {
      return "/dashboard";
    }
    return true;
  }

  if (!hasAccessToken()) {
    return "/login";
  }

  return true;
});

export default router;
