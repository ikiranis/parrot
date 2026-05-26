import {reactive} from 'vue';

interface ErrorData {
    enable: boolean;
    message: string;
    status: number;
    progress: number;
}

const state: ErrorData = reactive({
    enable: false,
    message: '',
    status: 0,
    progress: 0
});

/**
 * Sets the error/success notification state.
 *
 * @param enable  whether the alert should be visible
 * @param message text to display in the alert
 * @param status  HTTP status code that determined the alert type
 */
const set = (enable: boolean, message: string, status: number) => {
    const newState: ErrorData = {
        enable: enable,
        message: message,
        status: status,
        progress: 0
    };

    Object.assign(state, newState)
};

/**
 * Updates the auto-dismiss progress bar value.
 *
 * @param progress value from `0` (just opened) to `100+` (triggers close)
 */
const setProgress = (progress: number) => {
    state.progress = progress;
}

/**
 * Returns the reactive notification state object.
 *
 * @returns the current {@link ErrorData} state
 */
const get = () => {
    return state;
};

export const errorStore =  {
    set,
    get,
    setProgress
};
