import { motion } from 'motion/react'
import { useId } from 'react'

export interface SegmentOption<T extends string | number> {
  value: T
  label: string
}

interface Props<T extends string | number> {
  options: SegmentOption<T>[]
  value: T
  onChange: (value: T) => void
  size?: 'md' | 'sm'
  disabled?: boolean
  ariaLabel?: string
  className?: string
}

/** 선택 표시가 부드럽게 이동하는 세그먼트 컨트롤. 일일/주간 탭과 0~3 비중 선택에 쓴다. */
export function SegmentedTabs<T extends string | number>({
  options,
  value,
  onChange,
  size = 'md',
  disabled = false,
  ariaLabel,
  className = '',
}: Props<T>) {
  const layoutId = useId()
  const pad = size === 'sm' ? 'h-7 min-w-8 px-2 text-[13px]' : 'h-8 px-3 text-[13px]'
  return (
    <div
      role="group"
      aria-label={ariaLabel}
      className={`inline-flex rounded-lg bg-ink/5 p-0.5 ${disabled ? 'opacity-50' : ''} ${className}`}
    >
      {options.map((option) => {
        const selected = option.value === value
        return (
          <button
            key={option.value}
            type="button"
            aria-pressed={selected}
            disabled={disabled}
            onClick={() => onChange(option.value)}
            className={`relative rounded-md font-medium transition-colors ${pad} ${
              selected ? 'text-white' : 'text-charcoal hover:text-ink'
            }`}
          >
            {selected && (
              <motion.span
                layoutId={layoutId}
                className="absolute inset-0 rounded-md bg-prussian"
                transition={{ type: 'spring', bounce: 0.15, duration: 0.35 }}
              />
            )}
            <span className="relative">{option.label}</span>
          </button>
        )
      })}
    </div>
  )
}
