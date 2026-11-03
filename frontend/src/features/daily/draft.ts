import type { DayView } from '../../api/types'
import type { SaveInput } from './useDay'

/** 저장 전에 화면에서 편집 중인 값. 항목 id별 비중과 메모를 들고 있다. */
export interface Draft {
  date: string
  memo: string
  rows: Record<number, { weight: number; memo: string }>
}

export function toDraft(day: DayView): Draft {
  const rows: Draft['rows'] = {}
  day.entries.forEach((e) => (rows[e.itemId] = { weight: e.weight, memo: e.memo ?? '' }))
  day.waitingItems.forEach((w) => (rows[w.itemId] = { weight: 0, memo: '' }))
  return { date: day.date, memo: day.memo ?? '', rows }
}

export function toSaveInput(draft: Draft): SaveInput {
  return {
    memo: draft.memo,
    entries: Object.entries(draft.rows).map(([id, row]) => ({
      itemId: Number(id),
      weight: row.weight,
      memo: row.memo,
    })),
  }
}
