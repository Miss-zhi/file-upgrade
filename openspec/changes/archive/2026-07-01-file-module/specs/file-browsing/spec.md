## ADDED Requirements

### Requirement: File list by path
The system SHALL support listing files via `GET /api/v1/file/getfilelist`. The request MUST accept filePath, optional fileType filter, currentPage, and pageCount parameters. The system SHALL return a paginated list of non-deleted files and folders in the specified path, belonging to the authenticated user. Results MUST be sorted with folders first, then files alphabetically.

#### Scenario: List root directory
- **WHEN** authenticated user requests file list for filePath "/"
- **THEN** the system returns a paginated list of all non-deleted files and folders in the root directory, folders listed first

#### Scenario: List with file type filter
- **WHEN** user requests file list with fileType "image"
- **THEN** the system returns only files with image extensions (jpg, png, gif, bmp, webp) from the specified path

#### Scenario: List with pagination
- **WHEN** user requests page 2 with pageCount 20
- **THEN** the system returns items 21-40 of the filtered list, along with total count for pagination UI

#### Scenario: List empty directory
- **WHEN** user requests file list for an empty directory
- **THEN** the system returns an empty list with total count 0

### Requirement: File list by type (category view)
The system SHALL support listing files across all directories by file type category. When filePath is omitted or set to a special category value, the system SHALL return all non-deleted files of that type across the user's entire file system, with pagination.

#### Scenario: List all images across directories
- **WHEN** user requests file list with fileType "image" and no specific filePath
- **THEN** the system returns all image files across all directories, paginated

#### Scenario: List all documents across directories
- **WHEN** user requests file list with fileType "document"
- **THEN** the system returns all document files (docx, pdf, xlsx, pptx, txt, md) across all directories, paginated

#### Scenario: List all files (no filter)
- **WHEN** user requests file list without fileType filter
- **THEN** the system returns all non-deleted files and folders in the specified path

### Requirement: 按文件类型分类浏览
The system SHALL support category-based file browsing via `GET /api/v1/file/getfilelist/bycategory?category={category}`. The `category` parameter MUST be one of the supported categories: image, document, video, audio, archive, other. Extension-to-category mappings SHALL be defined in the `FileCategory` enum. The system SHALL return a paginated file list filtered by the extension group corresponding to the requested category, across all directories for the authenticated user.

#### Scenario: Browse files by image category
- **WHEN** user requests files with category "image"
- **THEN** the system returns a paginated list of all files whose extensions match the image group defined in `FileCategory` enum (e.g., jpg, png, gif, bmp, webp)

#### Scenario: Browse files by document category
- **WHEN** user requests files with category "document"
- **THEN** the system returns a paginated list of all files whose extensions match the document group defined in `FileCategory` enum (e.g., docx, pdf, xlsx, pptx, txt, md)

#### Scenario: Browse files by unsupported or unknown category
- **WHEN** user requests files with an unrecognized category value
- **THEN** the system returns an error indicating the category is not supported

#### Scenario: Category browsing with pagination
- **WHEN** user requests category "video" with currentPage=1 and pageCount=20
- **THEN** the system returns up to 20 video files with pagination metadata (total count, current page, page size)

### Requirement: File tree
The system SHALL support retrieving the user's folder tree via `GET /api/v1/file/getfiletree`. The response SHALL be a hierarchical tree structure of folders only, suitable for rendering in a tree selector (e.g., for move/copy target selection).

#### Scenario: Retrieve full folder tree
- **WHEN** authenticated user requests the file tree
- **THEN** the system returns a tree structure with root folder containing all non-deleted subfolders in nested hierarchy, each node containing folderId, folderName, and children array

#### Scenario: File tree excludes deleted items
- **WHEN** user has deleted some folders
- **THEN** the file tree MUST NOT include any soft-deleted items (deleteStatus=1)

#### Scenario: Deep folder hierarchy
- **WHEN** user has folders nested 5 levels deep
- **THEN** the tree correctly represents the full depth hierarchy without truncation

### Requirement: File list response format
The file list response SHALL use `FileListVO` containing: userFileId, fileName, filePath, extendName, fileType (1=file, 2=folder), fileSize, uploadTime, and a downloadUrl for preview purposes. The response MUST be wrapped in `RestResult<T>` with pagination metadata (total count, current page, page size).

#### Scenario: Response includes pagination metadata
- **WHEN** user requests a file list with currentPage=1 and pageCount=20, and there are 50 items
- **THEN** the response includes totalCount=50, currentPage=1, pageCount=20, and 20 items in the data array

#### Scenario: Response wraps in RestResult
- **WHEN** any file list request succeeds
- **THEN** the response is wrapped in RestResult with code=0, message="success", and the data in the data field
