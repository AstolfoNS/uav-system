import { reactive } from "vue";

type ScrollEdgeState = {
  atTop: boolean;
  atBottom: boolean;
};

type UseScrollEdgeStateOptions = {
  threshold?: number;
};

function normalizeThreshold(value: number | undefined): number {
  if (!Number.isFinite(value)) {
    return 2;
  }

  return Math.max(0, Number(value));
}

function computeScrollState(
  target: HTMLElement,
  threshold: number,
): ScrollEdgeState {
  const atTop = target.scrollTop <= threshold;
  const atBottom =
    target.scrollHeight - target.scrollTop - target.clientHeight <= threshold;
  return { atTop, atBottom };
}

export function useScrollEdgeState<K extends string>(
  keys: readonly K[],
  options?: UseScrollEdgeStateOptions,
) {
  const threshold = normalizeThreshold(options?.threshold);

  const scrollState = reactive(
    Object.fromEntries(
      keys.map((key) => [key, { atTop: true, atBottom: true }]),
    ) as Record<K, ScrollEdgeState>,
  ) as unknown as Record<K, ScrollEdgeState>;

  function refreshByElement(key: K, element: HTMLElement | null): void {
    if (!element) {
      scrollState[key].atTop = true;
      scrollState[key].atBottom = true;
      return;
    }

    const nextState = computeScrollState(element, threshold);
    scrollState[key].atTop = nextState.atTop;
    scrollState[key].atBottom = nextState.atBottom;
  }

  function refreshByResolver(
    key: K,
    resolver: (key: K) => HTMLElement | null,
  ): void {
    refreshByElement(key, resolver(key));
  }

  function refreshAllByResolver(
    resolver: (key: K) => HTMLElement | null,
  ): void {
    keys.forEach((key) => {
      refreshByElement(key, resolver(key));
    });
  }

  function handleScroll(key: K, event: Event): void {
    const target = event.target as HTMLElement | null;
    if (!target) {
      return;
    }

    const nextState = computeScrollState(target, threshold);
    scrollState[key].atTop = nextState.atTop;
    scrollState[key].atBottom = nextState.atBottom;
  }

  return {
    scrollState,
    refreshByElement,
    refreshByResolver,
    refreshAllByResolver,
    handleScroll,
  };
}
