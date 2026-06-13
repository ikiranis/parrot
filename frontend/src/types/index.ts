
/** Spring Data Page response envelope returned by paginated API endpoints. */
export type PageResponse<T> = {
	content: T[]
	totalElements: number
	totalPages: number
	number: number
	last: boolean
	first: boolean
	size: number
}

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

/**
 * Search criteria describing which photos a search or slideshow should match.
 *
 * Carries the free-text query and an optional exact rating filter. Passed as a single JSON
 * object through the slideshow route and the photo-batch API so the slideshow can play exactly
 * the photos currently shown in the search results. Designed to grow: future tag-based filters
 * can be added as additional optional fields without changing the APIs that pass it through.
 */
export type PhotoQuery = {
	/** Free-text query matched against photo paths and filenames; empty matches all photos. */
	text: string
	/** Exact rating filter (1–5), or null to match all ratings. */
	rating: number | null
}

/** A media file entry as returned by the API. */
export type MediaFile = {
	id: number
	path: string
	filename: string
	hash: string | null
	kind: string
	thumbnailId: number | null
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
	level: number
	finished: boolean
	lastUpdate: string | null
	thumbnailId: number | null
	libraryFolder: {
		id: number
		path: string
	}
}

/** A configured library folder used as a scan source. */
export type LibraryFolder = {
	id: number
	path: string
}

/** A single entry in the tag export/import payload. */
export type TagExportItem = {
	path: string
	filename: string
	rating: number | null
	viewCount: number | null
}

/** Result returned by the tag import endpoint. */
export type TagImportResult = {
	updated: number
	notFound: number
}

/** An application configuration setting as returned by the API. */
export type Setting = {
	id: number
	settingName: string
	settingValue: string
}

/** Lifecycle states for a background library scan job. */
export type ScanStatus = 'IDLE' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'CANCELLED'

/** Active scanning phase within a running background scan job. */
export type ScanPhase = 'COLLECTING' | 'SCANNING' | 'TAGGING'

/** Response from the background scan API endpoints. */
export type ScanJobResponse = {
	jobId: string | null
	status: ScanStatus
	/** Active scanning phase; null when idle or not yet started. */
	phase: ScanPhase | null
	startedAt: string | null
	completedAt: string | null
	added: number
	skipped: number
	errors: number
	foldersScanned: number
	foldersSkipped: number
	tagged: number
	/** Total leaf directories discovered in Phase 1. */
	totalFolders: number
	/** Total new files to tag discovered in Phase 2. */
	totalFiles: number
	/** Total media files found in the filesystem across all leaf directories after Phase 1. */
	totalMediaFilesInLibrary: number
	/** Estimated progress percentage in the range [0, 100]. */
	progressPercent: number
	/** Ordered list of error messages collected during the scan. */
	errorLogs: string[]
	/** Media files already in the database before this scan started. */
	initialFilesCount: number
	message: string
}

