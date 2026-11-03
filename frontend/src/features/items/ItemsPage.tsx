import { useState } from 'react'
import { ApiError } from '../../api/client'
import type { ItemStatus, WorkItem } from '../../api/types'
import { Card, SectionTitle } from '../../components/Card'
import { ItemForm } from './ItemForm'
import { ItemRow } from './ItemRow'
import { LeaveSection } from './LeaveSection'
import { SettingsSection } from './SettingsSection'
import {
  useChangeStatus,
  useCreateItem,
  useDeleteItem,
  useItems,
  useReorder,
  useTags,
  useUpdateItem,
} from './useItems'

const HAS_ENTRIES_HINT = '기록이 있어 삭제할 수 없어요. 완료 처리해 주세요.'

export function ItemsPage() {
  const items = useItems()
  const tags = useTags()
  const create = useCreateItem()
  const update = useUpdateItem()
  const changeStatus = useChangeStatus()
  const remove = useDeleteItem()
  const reorder = useReorder()

  const [form, setForm] = useState<'new' | WorkItem | null>(null)
  const [hintId, setHintId] = useState<number | null>(null)
  const [showDone, setShowDone] = useState(false)

  const list = items.data ?? []
  const groups: { status: ItemStatus; title: string }[] = [
    { status: 'IN_PROGRESS', title: '진행 중' },
    { status: 'WAITING', title: '대기' },
  ]
  const done = list.filter((i) => i.status === 'DONE')

  function move(item: WorkItem, direction: -1 | 1) {
    const siblings = list.filter((i) => i.status === item.status)
    const neighbor = siblings[siblings.findIndex((i) => i.id === item.id) + direction]
    if (!neighbor) return
    const ids = list.map((i) => i.id)
    const a = ids.indexOf(item.id)
    const b = ids.indexOf(neighbor.id)
    ;[ids[a], ids[b]] = [ids[b], ids[a]]
    reorder.mutate(ids)
  }

  function onDelete(item: WorkItem) {
    if (!window.confirm(`'${item.title}' 항목을 삭제할까요?`)) return
    setHintId(null)
    remove.mutate(item.id, {
      onError: (e) => {
        if (e instanceof ApiError && e.code === 'HAS_ENTRIES') setHintId(item.id)
      },
    })
  }

  function renderRows(rows: WorkItem[]) {
    return rows.map((item, index) => (
      <ItemRow
        key={item.id}
        item={item}
        canUp={index > 0}
        canDown={index < rows.length - 1}
        hint={hintId === item.id ? HAS_ENTRIES_HINT : undefined}
        onEdit={() => setForm(item)}
        onMove={(d) => move(item, d)}
        onStatus={(status) => changeStatus.mutate({ id: item.id, status })}
        onDelete={() => onDelete(item)}
      />
    ))
  }

  return (
    <div>
      <div className="flex items-center justify-between">
        <h1 className="text-[26px] font-semibold tracking-tight">업무</h1>
        <button
          type="button"
          onClick={() => setForm('new')}
          className="h-9 rounded-xl bg-prussian px-4 text-sm font-semibold text-white hover:bg-prussian/90"
        >
          새 업무
        </button>
      </div>

      {form && (
        <ItemForm
          key={form === 'new' ? 'new' : form.id}
          initial={form === 'new' ? undefined : form}
          tags={tags.data ?? []}
          pending={create.isPending || update.isPending}
          error={create.error?.message ?? update.error?.message}
          onCancel={() => setForm(null)}
          onCreate={(input) => create.mutate(input, { onSuccess: () => setForm(null) })}
          onUpdate={(input) => update.mutate(input, { onSuccess: () => setForm(null) })}
        />
      )}

      {items.isError && <p className="mt-4 text-sm text-red-700">업무 항목을 불러오지 못했어요.</p>}

      {groups.map(({ status, title }) => {
        const rows = list.filter((i) => i.status === status)
        return (
          <section key={status}>
            <SectionTitle>{`${title} (${rows.length})`}</SectionTitle>
            <Card className="divide-y divide-line">
              {rows.length === 0 ? <p className="px-4 py-5 text-center text-[13px] text-mute">없음</p> : renderRows(rows)}
            </Card>
          </section>
        )
      })}

      <section>
        <SectionTitle>
          <button type="button" onClick={() => setShowDone((v) => !v)} aria-expanded={showDone}>
            {`완료 (${done.length}) ${showDone ? '▴' : '▾'}`}
          </button>
        </SectionTitle>
        {showDone && <Card className="divide-y divide-line">{renderRows(done)}</Card>}
      </section>

      <LeaveSection />
      <SettingsSection />
    </div>
  )
}
