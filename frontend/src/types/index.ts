
export interface Pageable {
    offset: number;
    pageNumber: number;
    pageSize: number;
    paged: boolean;
    sort: Sort;
    unpaged: boolean;
    totalPages: number;
    sortField: SortField
}

export type Sort = {
    empty: boolean;
    sorted: boolean;
    unsorted: boolean;
}

export type SortField = {
    field: string;
    order: string;
    enable: boolean;
}

export type Language = {
    lang: string,
    name: string
}

export type ScanResult = {
    added: number
    skipped: number
    errors: number
    message: string
}

export type MediaFile = {
    id: number
    path: string
    filename: string
    hash: string | null
    kind: string
}

