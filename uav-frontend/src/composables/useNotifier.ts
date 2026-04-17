import { reactive } from "vue";

type MessageType = "success" | "info" | "warning" | "error";

export const notifierState = reactive({
  show: false,
  text: "",
  color: "info" as MessageType,
  timeout: 2500,
});

export function notify(
  message: string,
  type: MessageType = "info",
  timeout = 2500,
): void {
  notifierState.text = message;
  notifierState.color = type;
  notifierState.timeout = timeout;
  notifierState.show = true;
}
