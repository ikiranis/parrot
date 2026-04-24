import {ref, Ref} from "vue";
import {SortField} from "@/types";

const sortFields: Ref<Array<SortField>> = ref([])

const setSortFields = (sortFieldsValues: Array<SortField>) => {
    sortFields.value = sortFieldsValues
}

const getSortFields = () => {
    return sortFields.value
}

/**
 * Enable the sort field
 *
 * @param index
 */
const enableSortField = (index: number) => {
    // If the field is already enabled, swap the sort order
    if (sortFields.value[index].enable) {
        swapSortOrder(index)
        return
    }

    sortFields.value[index].enable = !sortFields.value[index].enable

    // disable the other fields
    sortFields.value.forEach((field, i) => {
        if (i !== index) {
            field.enable = false
        }
    })
}

/**
 * Swap the sort order
 *
 * @param index
 */
const swapSortOrder = (index: number) => {
    if (sortFields.value[index].order === 'asc') {
        sortFields.value[index].order = 'desc'
    } else {
        sortFields.value[index].order = 'asc'
    }
}

/**
 * Get the enabled sort field
 */
const getEnabledSortField = () => {
    return sortFields.value.find(field => field.enable) as SortField
}

const getSortField = (field: string) : SortField => {
    return sortFields.value.find(sortField => sortField.field === field) as SortField
}

const getSortFieldIndex = (field: string) : number => {
    return sortFields.value.findIndex(sortField => sortField.field === field)
}

export const sortFieldsStore =  {
    setSortFields,
    getSortFields,
    enableSortField,
    getEnabledSortField,
    getSortField,
    getSortFieldIndex
};
