/**
 * This code defines a useDebouncedRef function that allows a developer to create a ref with a debounced value.
 * This means that if the ref's value is set multiple times within a given time period (specified by the
 * delay parameter), the ref's value will only be updated after the time period has elapsed without any
 * additional updates. This can be useful for preventing rapid updates to a value that may be expensive
 * to compute or otherwise unnecessary. The useDebouncedRef function uses the debounce helper function,
 * which is also defined in this code, to implement the debouncing behavior.
 *
 * Code from:
 * https://theroadtoenterprise.com/blog/how-to-create-a-debounced-ref-in-vue-3-using-composition-api
 */

import {ref, customRef} from 'vue'

type AnyFn = (...args: unknown[]) => unknown

/**
 * Returns a debounced version of `fn` that only fires after `delay` ms of inactivity.
 *
 * @param fn        function to debounce
 * @param delay     debounce window in milliseconds
 * @param immediate fire immediately on the first call within the window
 * @returns the debounced function
 */
const debounce = (fn: AnyFn, delay: number = 0, immediate: boolean = false) => {
    let timeout: ReturnType<typeof setTimeout> | undefined
    return (...args: unknown[]) => {
        if (immediate && !timeout) fn(...args)
        clearTimeout(timeout)

        timeout = setTimeout(() => {
            fn(...args)
        }, delay)
    }
}

/**
 * Creates a Vue `customRef` whose setter is debounced.
 *
 * @param initialValue the starting value for the ref
 * @param delay        debounce window in milliseconds
 * @param immediate    fire immediately on the leading edge of the window
 * @returns a Vue ref that debounces its setter
 */
const useDebouncedRef = <T>(initialValue: T, delay: number, immediate: boolean) => {
    const state = ref(initialValue)
    return customRef((track, trigger) => ({
        get() {
            track()
            return state.value
        },
        set: debounce(
            (value: unknown) => {
                state.value = value as T
                trigger()
            },
            delay,
            immediate
        ),
    }))
}

export default useDebouncedRef
