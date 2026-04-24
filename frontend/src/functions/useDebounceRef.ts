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

const debounce = (fn: any, delay: number = 0, immediate: boolean = false) => {
    let timeout: any
    return (...args: any[]) => {
        if (immediate && !timeout) fn(...args)
        clearTimeout(timeout)

        timeout = setTimeout(() => {
            fn(...args)
        }, delay)
    }
}

const useDebouncedRef = (initialValue: any, delay: any, immediate: boolean) => {
    const state = ref(initialValue)
    return customRef((track, trigger) => ({
        get() {
            track()
            return state.value
        },
        set: debounce(
            (value: any) => {
                state.value = value
                trigger()
            },
            delay,
            immediate
        ),
    }))
}

export default useDebouncedRef
