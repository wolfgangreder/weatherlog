export async function uploadFiles(files: File[]): Promise<void> {
  const form = new FormData()
  files.forEach(f => form.append('file', f, f.name))
  const res = await fetch('/weather/heatpump', { method: 'POST', body: form })
  if (!res.ok) {
    throw new Error(`Upload failed: ${res.status} ${res.statusText}`)
  }
}
