import {ref, Ref} from "vue";
import {SortField} from "@/types";

const sortFields: Ref<Array<SortField>> = ref([])

/**
 * Replaces the entire sort-fields store with the given array.
 *
 * @param sortFieldsValues new sort-field definitions
 */
const setSortFields = (sortFieldsValues: Array<SortField>) => {
    sortFields.value = sortFieldsValues
}

/**
 * Returns all sort-field definitions currently in the store.
 *
 * @returns array of {@link SortField} entries
 */
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

/**
 * Finds the sort-field definition for the given column name.
 *
 * @param field the column/field name to look up
 * @returns the matching {@link SortField}
 */
const getSortField = (field: string) : SortField => {
    return sortFields.value.find(sortField => sortField.field === field) as SortField
}

/**
 * Returns the array index of the sort-field with the given column name.
 *
 * @param field the column/field name to search for
 * @returns the zero-based index, or `-1` if not found
 */
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
