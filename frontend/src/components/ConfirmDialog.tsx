import { AnimatePresence, motion } from 'motion/react'

interface Props {
  open: boolean
  title: string
  message: string
  confirmLabel?: string
  onConfirm: () => void
  onCancel: () => void
}

export function ConfirmDialog({ open, title, message, confirmLabel = '계속', onConfirm, onCancel }: Props) {
  return (
    <AnimatePresence>
      {open && (
        <motion.div
          className="fixed inset-0 z-40 grid place-items-center bg-ink/30 px-6"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
        >
          <motion.div
            role="dialog"
            aria-label={title}
            initial={{ opacity: 0, scale: 0.96 }}
            animate={{ opacity: 1, scale: 1 }}
            exit={{ opacity: 0, scale: 0.96 }}
            className="w-full max-w-sm rounded-2xl bg-white p-5 shadow-xl"
          >
            <h2 className="text-[17px] font-semibold">{title}</h2>
            <p className="mt-2 text-sm text-charcoal">{message}</p>
            <div className="mt-5 flex justify-end gap-2">
              <button type="button" onClick={onCancel} className="h-9 rounded-lg px-4 text-sm text-charcoal hover:bg-canvas">
                취소
              </button>
              <button
                type="button"
                onClick={onConfirm}
                className="h-9 rounded-lg bg-prussian px-4 text-sm font-semibold text-white hover:bg-prussian/90"
              >
                {confirmLabel}
              </button>
            </div>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  )
}
