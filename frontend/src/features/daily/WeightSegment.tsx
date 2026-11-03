import { SegmentedTabs } from '../../components/SegmentedTabs'

const OPTIONS = [0, 1, 2, 3].map((n) => ({ value: n, label: String(n) }))

export function WeightSegment({
  title,
  value,
  onChange,
  disabled,
}: {
  title: string
  value: number
  onChange: (weight: number) => void
  disabled?: boolean
}) {
  return (
    <SegmentedTabs
      size="sm"
      ariaLabel={`${title} 비중`}
      options={OPTIONS}
      value={value}
      onChange={onChange}
      disabled={disabled}
    />
  )
}
