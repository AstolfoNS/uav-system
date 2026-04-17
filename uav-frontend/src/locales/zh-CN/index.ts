import app from "./app";
import common from "./common";
import dashboard from "./dashboard";
import errors from "./errors";
import inference from "./inference";
import login from "./login";
import node from "./node";
import profile from "./profile";
import records from "./records";

// Key prefix convention:
// - app.* from app.ts
// - common.* from common.ts
// - dashboard.* from dashboard.ts
// - errors.* from errors.ts
// - inference.* from inference.ts
// - login.* from login.ts
// - node.* from node.ts
// - profile.* from profile.ts
// - records.* from records.ts
const zhCN = {
  app,
  common,
  dashboard,
  errors,
  inference,
  login,
  node,
  profile,
  records,
};

export default zhCN;
