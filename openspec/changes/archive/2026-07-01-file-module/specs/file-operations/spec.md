## ADDED Requirements

### Requirement: Create file
The system SHALL support creating new files via `POST /api/v1/file/createFile`. The request MUST include fileName, filePath, and extendName. The system SHALL use UFOP Copier to copy a template file from the configured template storage, create a FileBean, and create a UserFile record. The system MUST reject creation if a file with the same name and extension already exists in the target path.

#### Scenario: Successful file creation
- **WHEN** authenticated user creates a new file with a unique name in the target directory
- **THEN** the system creates a FileBean and UserFile record, and returns success with the new userFileId

#### Scenario: Duplicate file name on creation
- **WHEN** user creates a file with the same name and extension as an existing non-deleted file in the same path
- **THEN** the system returns an error indicating the file already exists

### Requirement: Create folder
The system SHALL support creating folders via `POST /api/v1/file/createFold`. The request MUST include folderName and filePath. The system MUST reject creation if a folder with the same name already exists in the target path.

#### Scenario: Successful folder creation
- **WHEN** authenticated user creates a new folder with a unique name
- **THEN** the system creates a UserFile record with fileType=2 and returns success

#### Scenario: Duplicate folder name
- **WHEN** user creates a folder that already exists in the same path
- **THEN** the system returns an error indicating the folder already exists

### Requirement: Rename file or folder
The system SHALL support renaming via `POST /api/v1/file/renamefile`. The request MUST include userFileId and the new fileName. When renaming a folder, the system MUST recursively update the filePath of all child files and subfolders. The system MUST reject the rename if the new name conflicts with an existing file in the same directory.

#### Scenario: Successful file rename
- **WHEN** user renames a file to a name that does not conflict with siblings
- **THEN** the system updates the fileName and returns success

#### Scenario: Rename folder with children
- **WHEN** user renames a folder from "oldName" to "newName"
- **THEN** the system updates the folder's fileName AND recursively updates filePath of all descendant files and folders from "/.../oldName/..." to "/.../newName/..."

#### Scenario: Rename conflict
- **WHEN** user renames a file to a name that already exists in the same directory
- **THEN** the system returns an error indicating name conflict

### Requirement: Move file
The system SHALL support moving files via `POST /api/v1/file/movefile`. The request MUST include userFileId and targetPath. The system MUST check for name conflicts in the target directory. Moving a file to its current path is a no-op success.

#### Scenario: Successful move
- **WHEN** user moves a file to a different directory where no name conflict exists
- **THEN** the system updates the file's filePath and returns success

#### Scenario: Move with name conflict
- **WHEN** user moves a file to a directory where a file with the same name already exists
- **THEN** the system returns an error indicating name conflict

#### Scenario: Move to same location
- **WHEN** user moves a file to its current directory
- **THEN** the system returns success without modifying the record

### Requirement: Batch move files
The system SHALL support batch moving via `POST /api/v1/file/batchmovefile`. The request MUST include a list of userFileIds and targetPath. Each file is moved independently; partial failures MUST report which files succeeded and which failed. Moving a folder MUST recursively update all descendant filePath values.

#### Scenario: Successful batch move
- **WHEN** user batch moves 5 files to a target directory with no conflicts
- **THEN** all 5 files are moved successfully, response returns success count = 5

#### Scenario: Partial failure in batch move
- **WHEN** user batch moves 5 files but 2 have name conflicts in target directory
- **THEN** 3 files are moved successfully, and the response indicates which 2 files failed with conflict errors (HTTP 409 per file)

#### Scenario: Batch move folder with descendants
- **WHEN** user batch moves a folder containing sub-items
- **THEN** folder and all descendants have their filePath updated atomically within a single transaction

### Requirement: Copy file
The system SHALL support copying files via `POST /api/v1/file/copyfile`. The request MUST include userFileId and targetPath. The system SHALL create a new UserFile record in the target directory. If the source is backed by a FileBean, the copy reuses the same FileBean (content-addressed deduplication). The system MUST check for name conflicts in the target directory.

#### Scenario: Successful file copy
- **WHEN** user copies a file to a different directory
- **THEN** the system creates a new UserFile record pointing to the same FileBean, and returns success with the new userFileId

#### Scenario: Copy with name conflict
- **WHEN** user copies a file to a directory where a file with the same name exists
- **THEN** the system returns HTTP 409 with error indicating name conflict

#### Scenario: Copy folder
- **WHEN** user copies a folder
- **THEN** the system creates new UserFile records for the folder and all its descendants, each referencing the same FileBean as the original

### Requirement: Batch copy files
The system SHALL support batch copying via `POST /api/v1/file/batchcopyfile`. The request MUST include a list of userFileIds and targetPath. Each file is copied independently; partial failures MUST report which files succeeded and which failed.

#### Scenario: Successful batch copy
- **WHEN** user batch copies 5 files to a target directory with no conflicts
- **THEN** all 5 files are copied successfully, response returns success count = 5

#### Scenario: Partial failure in batch copy
- **WHEN** user batch copies 5 files but 2 have name conflicts
- **THEN** 3 files are copied successfully, and the response indicates which 2 files failed with conflict errors

### Requirement: Batch delete files
The system SHALL support batch soft-deletion via `POST /api/v1/file/batchdeletefile`. The request MUST include a list of userFileIds. Each file is soft-deleted independently within a single transaction, sharing the same deleteBatchNum.

#### Scenario: Successful batch delete
- **WHEN** user batch deletes 3 files
- **THEN** all 3 files are soft-deleted with the same deleteBatchNum, and the response returns success count = 3

#### Scenario: Batch delete with folders
- **WHEN** user batch deletes items that include a folder with children
- **THEN** the folder and all its descendants are soft-deleted with the same deleteBatchNum

### Requirement: Soft delete file
The system SHALL support soft deletion via `POST /api/v1/file/deletefile`. The request MUST include userFileId. The system SHALL set deleteStatus=1 and record deleteTime and deleteBatchNum. The file SHALL no longer appear in normal file listings but SHALL be visible in the recovery file list. If the deleted file is a folder, all children MUST also be soft-deleted recursively.

#### Scenario: Successful soft delete
- **WHEN** user deletes a file
- **THEN** the file's deleteStatus is set to 1, deleteTime is recorded, and the file no longer appears in normal listings

#### Scenario: Delete folder with children
- **WHEN** user deletes a folder containing 3 files and 1 subfolder with 2 files
- **THEN** the folder and all 6 descendant items are soft-deleted with the same deleteBatchNum

### Requirement: File detail query
The system SHALL support querying file details via `GET /api/v1/file/detail?userFileId={id}`. The response SHALL aggregate UserFile metadata, FileBean metadata, and media-specific metadata (image dimensions, music duration) when applicable.

#### Scenario: Query regular file detail
- **WHEN** user queries details of a regular file
- **THEN** the system returns FileDetailVO with fileName, fileSize, extendName, filePath, uploadTime, storageType, and identifier

#### Scenario: Query image file detail
- **WHEN** user queries details of an image file
- **THEN** the system returns FileDetailVO including image-specific metadata (dimensions) if available
