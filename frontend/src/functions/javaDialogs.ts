declare global {
    interface Window {
        appController: {
            confirmDialog: (promptText: string) => boolean;
            saveDialog: (url: string | ArrayBuffer | null, filename: string) => boolean;
        };
    }
}

/**
 * Use javaFX confirm or browser confirm
 *
 * @param message
 */
export const confirmDialog = (message: string): boolean => {
    if (typeof window.appController !== "undefined") {
        return window.appController.confirmDialog(message)
    }

    return confirm(message)
}

/**
 * Use javaFX save dialog or browser save dialog, to download a file
 *
 * @param response
 * @param filename
 */
export const saveDialog = (response: any, filename: string) => {
    // Trigger a click event on the anchor to initiate the download
    if (typeof window.appController !== "undefined") { // When on javaFX
        // Create a Blob from the binary data
        const blob = new Blob([response.data], {type: response.headers['content-type']});

        // Read the Blob as a data URL
        const reader = new FileReader();
        reader.onload = () => {
            const dataUrl = reader.result;
            window.appController.saveDialog(dataUrl, filename)
        };

        reader.readAsDataURL(blob);
    } else { // When on regular browser
        // Create a Blob from the binary data
        const blob = new Blob([response.data], {type: response.headers['content-type']});

        // Create a temporary anchor element
        const anchor = document.createElement('a');
        anchor.href = window.URL.createObjectURL(blob);
        anchor.download = filename || 'file';

        anchor.click();

        // Clean up
        window.URL.revokeObjectURL(anchor.href);
    }

}
