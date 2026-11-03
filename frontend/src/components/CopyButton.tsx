import { useToast } from './Toast'

interface Props {
  text: string
  label?: string
  primary?: boolean
}

export function CopyButton({ text, label = '복사', primary = false }: Props) {
  const toast = useToast()

  async function copy() {
    try {
      await navigator.clipboard.writeText(text)
      toast('복사했어요')
    } catch {
      toast('복사하지 못했어요')
    }
  }

  return (
    <button
      type="button"
      onClick={copy}
      className={`h-8 rounded-lg px-3 text-[13px] font-medium transition-colors ${
        primary
          ? 'bg-prussian text-white hover:bg-prussian/90'
          : 'border border-line bg-white text-charcoal hover:bg-canvas'
      }`}
    >
      {label}
    </button>
  )
}
