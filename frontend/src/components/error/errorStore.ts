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

const set = (enable: boolean, message: string, status: number) => {
    const newState: ErrorData = {
        enable: enable,
        message: message,
        status: status,
        progress: 0
    };

    Object.assign(state, newState)
};

const setProgress = (progress: number) => {
    state.progress = progress;
}

const get = () => {
    return state;
};

export const errorStore =  {
    set,
    get,
    setProgress
};
