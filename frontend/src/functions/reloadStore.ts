import { ref, Ref } from "vue"

/**
 * Monotonic key used to force a remount of the active router view.
 *
 * Some views (e.g. the photo grid and slideshow) navigate through folders by
 * mutating internal state without changing the route, so a router-link back to
 * the same route is a no-op and leaves the view drilled in. Binding this key to
 * the router-view and bumping it lets a menu click reset and reload the view
 * even when the resolved route is unchanged.
 */
const reloadKey: Ref<number> = ref(0)

/**
 * Bumps {@link reloadKey}, forcing the active router view to remount and reload
 * from its initial state.
 */
const triggerReload = (): void => {
	reloadKey.value++
}

export { reloadKey, triggerReload }
