export type DataType = 'System' | 'Zone' | 'Energy' | 'HotWater'

export function detectFileType(filename: string): DataType | null {
  const lower = filename.toLowerCase()
  if (lower.startsWith('domestic_hot_water')) return 'HotWater'
  if (lower.startsWith('system'))            return 'System'
  if (lower.startsWith('zone'))             return 'Zone'
  if (lower.startsWith('energy'))           return 'Energy'
  return null
}

export function labelForType(type: DataType): string {
  const labels: Record<DataType, string> = {
    System:   'System',
    Zone:     'Zone',
    Energy:   'Energy',
    HotWater: 'Hot Water',
  }
  return labels[type]
}

export function colorForType(type: DataType): 'green' | 'purple' | 'blue' | 'teal' {
  const colors: Record<DataType, 'green' | 'purple' | 'blue' | 'teal'> = {
    System:   'green',
    Zone:     'purple',
    Energy:   'blue',
    HotWater: 'teal',
  }
  return colors[type]
}
