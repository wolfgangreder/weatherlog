import React, { useState } from 'react'
import {
  Alert,
  AlertActionCloseButton,
  AlertGroup,
  Button,
  Card,
  CardBody,
  CardTitle,
  Flex,
  FlexItem,
  Label,
  MultipleFileUpload,
  MultipleFileUploadMain,
  MultipleFileUploadStatus,
  MultipleFileUploadStatusItem,
} from '@patternfly/react-core'
import { CloudUploadAltIcon } from '@patternfly/react-icons'
import { type DataType, colorForType, detectFileType, labelForType } from '../utils/detectFileType'
import { uploadFiles } from '../api/heatpumpApi'

interface FileEntry {
  file: File
  type: DataType | null
}

interface UploadResult {
  success: boolean
  message: string
}

const HeatpumpUpload: React.FC = () => {
  const [fileEntries, setFileEntries] = useState<FileEntry[]>([])
  const [uploading, setUploading] = useState(false)
  const [result, setResult] = useState<UploadResult | null>(null)

  const handleFileDrop = (_event: unknown, newFiles: File[]) => {
    setResult(null)
    setFileEntries(prev => {
      const existing = new Set(prev.map(e => e.file.name))
      const toAdd = newFiles
        .filter(f => !existing.has(f.name))
        .map(f => ({ file: f, type: detectFileType(f.name) }))
      return [...prev, ...toAdd]
    })
  }

  const removeFile = (name: string) => {
    setFileEntries(prev => prev.filter(e => e.file.name !== name))
  }

  const recognisedFiles = fileEntries.filter(e => e.type !== null)

  const handleUpload = async () => {
    setUploading(true)
    setResult(null)
    try {
      await uploadFiles(recognisedFiles.map(e => e.file))
      const typeLabels = recognisedFiles.map(e => labelForType(e.type!)).join(', ')
      setResult({
        success: true,
        message: `${recognisedFiles.length} file${recognisedFiles.length === 1 ? '' : 's'} uploaded successfully — ${typeLabels}`,
      })
      setFileEntries([])
    } catch (err) {
      setResult({
        success: false,
        message: err instanceof Error ? err.message : 'Upload failed',
      })
    } finally {
      setUploading(false)
    }
  }

  const allRecognised = fileEntries.length > 0 && recognisedFiles.length === fileEntries.length
  const statusIcon = allRecognised ? 'success' : 'warning'

  return (
    <Card style={{ maxWidth: 680, margin: '0 auto' }}>
      <CardTitle>Upload data files</CardTitle>
      <CardBody>
        {result && (
          <AlertGroup style={{ marginBottom: '1rem' }}>
            <Alert
              variant={result.success ? 'success' : 'danger'}
              title={result.message}
              actionClose={<AlertActionCloseButton onClose={() => setResult(null)} />}
            />
          </AlertGroup>
        )}

        <MultipleFileUpload onFileDrop={handleFileDrop}>
          <MultipleFileUploadMain
            titleIcon={<CloudUploadAltIcon />}
            titleText="Drag and drop CSV files here"
            titleTextSeparator="or"
            infoText="Accepted: system, zone, energy, domestic_hot_water files"
          />
          {fileEntries.length > 0 && (
            <MultipleFileUploadStatus
              statusToggleText={`${fileEntries.length} file${fileEntries.length === 1 ? '' : 's'} selected`}
              statusToggleIcon={statusIcon}
              aria-label="Uploaded files"
            >
              {fileEntries.map(entry => (
                <MultipleFileUploadStatusItem
                  key={entry.file.name}
                  file={entry.file}
                  onClearClick={() => removeFile(entry.file.name)}
                  customFileHandler={() => { /* prevent auto file-read */ }}
                  progressValue={100}
                  progressVariant={entry.type ? 'success' : 'warning'}
                  progressHelperText={
                    entry.type
                      ? <Label color={colorForType(entry.type)} isCompact>{labelForType(entry.type)}</Label>
                      : <Label color="orange" isCompact>Unknown — skipped</Label>
                  }
                />
              ))}
            </MultipleFileUploadStatus>
          )}
        </MultipleFileUpload>

        {fileEntries.length > 0 && (
          <Flex style={{ marginTop: '1rem' }}>
            <FlexItem>
              <Button
                variant="primary"
                onClick={handleUpload}
                isDisabled={recognisedFiles.length === 0 || uploading}
                isLoading={uploading}
              >
                {uploading
                  ? 'Uploading\u2026'
                  : `Upload ${recognisedFiles.length} file${recognisedFiles.length === 1 ? '' : 's'}`}
              </Button>
            </FlexItem>
            <FlexItem>
              <Button
                variant="secondary"
                onClick={() => { setFileEntries([]); setResult(null) }}
                isDisabled={uploading}
              >
                Clear all
              </Button>
            </FlexItem>
          </Flex>
        )}
      </CardBody>
    </Card>
  )
}

export default HeatpumpUpload
