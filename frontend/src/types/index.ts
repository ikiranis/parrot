
/** Pagination metadata returned by Spring Data paged responses. */
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

/** Sort state within a {@link Pageable} response. */
export type Sort = {
	empty: boolean;
	sorted: boolean;
	unsorted: boolean;
}

/** Describes a single sortable column and its current sort direction. */
export type SortField = {
	field: string;
	order: string;
	enable: boolean;
}

/** A supported UI language option. */
export type Language = {
	lang: string,
	name: string
}

/** Summary returned by a folder-scan operation. */
export type ScanResult = {
	added: number
	skipped: number
	errors: number
	foldersScanned: number
	foldersSkipped: number
	message: string
}

/** A media file entry as returned by the API. */
export type MediaFile = {
	id: number
	path: string
	filename: string
	hash: string | null
	kind: string
}

/** Combined photo detail including MediaFile fields and optional PhotoTag metadata. */
export type PhotoDetail = {
	id: number
	path: string
	filename: string
	hash: string | null
	kind: string
	name: string | null
	description: string | null
	album: string | null
	filesize: number | null
	width: number | null
	height: number | null
	viewCount: number | null
	rating: number | null
	dateTaken: string | null
	latitude: number | null
	longitude: number | null
	cameraMake: string | null
	cameraModel: string | null
	mimeType: string | null
	dateCreated: string | null
	dateUpdated: string | null
}

/** A scanned folder entry as returned by the API. */
export type Folder = {
	id: number
	path: string
	hash: string | null
	thumbnail: string | null
	lastUpdate: string | null
}

/** A configured library folder used as a scan source. */
export type LibraryFolder = {
	id: number
	path: string
}

/** An application configuration setting as returned by the API. */
export type Setting = {
	id: number
	settingName: string
	settingValue: string
}

/** Lifecycle states for a background library scan job. */
export type ScanStatus = 'IDLE' | 'RUNNING' | 'COMPLETED' | 'FAILED'

/** Response from the background scan API endpoints. */
export type ScanJobResponse = {
	jobId: string | null
	status: ScanStatus
	startedAt: string | null
	completedAt: string | null
	added: number
	skipped: number
	errors: number
	foldersScanned: number
	foldersSkipped: number
	tagged: number
	message: string
}

