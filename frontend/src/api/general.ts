import axios from "axios";
import config from "@/functions/config.ts";

/**
 * Check if the app is alive
 */
export const checkAppAlive = async (): Promise<boolean>  => {
    try {
        const response = await axios.get(config.defaultServer() + '/api/general/appAlive')

        if (response.status === 200) {
            return true
        }

        return false
    } catch(error: any) {
        return false
    }
}
