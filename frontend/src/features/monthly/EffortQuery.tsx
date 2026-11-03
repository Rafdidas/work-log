import { useState } from 'react'
import { Card } from '../../components/Card'
import { CopyButton } from '../../components/CopyButton'
import { formatMd } from '../../lib/md'
import { todaySeoul } from '../../lib/week'
import { STATUS_LABEL } from '../../api/types'
import { useItems, useTags } from '../items/useItems'
import { useEffort } from './useReports'

const fieldClass =
  'h-10 w-full rounded-xl border border-line bg-white px-3 text-[14px] outline-none focus:border-prussian focus:ring-2 focus:ring-prussian/15'

function monthLabel(month: string, sameYear: boolean) {
  return sameYear ? `${Number(month.slice(5))}월` : month
}

export function EffortQuery() {
  const items = useItems()
  const tags = useTags()
  const today = todaySeoul()
  const [target, setTarget] = useState('')
  const [from, setFrom] = useState(`${today.slice(0, 4)}-01`)
  const [to, setTo] = useState(today.slice(0, 7))
  const { data, isError, error } = useEffort(target, from, to)

  return (
    <div>
      <Card className="grid gap-3 p-4 sm:grid-cols-[1fr_auto_auto]">
        <label className="text-[13px] text-charcoal">
          대상
          <select aria-label="대상" className={`${fieldClass} mt-1`} value={target} onChange={(e) => setTarget(e.target.value)}>
            <option value="">선택하세요</option>
            {(tags.data?.length ?? 0) > 0 && (
              <optgroup label="서비스/고객사 태그">
                {tags.data!.map((t) => (
                  <option key={t} value={`tag:${t}`}>
                    {t}
                  </option>
                ))}
              </optgroup>
            )}
            <optgroup label="업무 항목">
              {items.data?.map((i) => (
                <option key={i.id} value={`item:${i.id}`}>
                  {i.status === 'DONE' ? `${i.title} (${STATUS_LABEL.DONE})` : i.title}
                </option>
              ))}
            </optgroup>
          </select>
        </label>
        <label className="text-[13px] text-charcoal">
          시작 월
          <input type="month" className={`${fieldClass} mt-1`} value={from} onChange={(e) => setFrom(e.target.value)} />
        </label>
        <label className="text-[13px] text-charcoal">
          종료 월
          <input type="month" className={`${fieldClass} mt-1`} value={to} onChange={(e) => setTo(e.target.value)} />
        </label>
      </Card>

      {from > to && <p className="mt-3 text-sm text-red-700">시작 월이 종료 월보다 늦어요.</p>}
      {isError && <p className="mt-3 text-sm text-red-700">{error.message}</p>}
      {target === '' && <p className="mt-4 px-1 text-[13px] text-mute">대상을 고르면 월별 투입 공수가 나와요.</p>}

      {data && (
        <section aria-label="공수 조회 결과" className="mt-4">
          <Card>
            <div className="flex items-center justify-between border-b border-line px-4 py-3">
              <h2 className="text-[15px] font-semibold">{`${data.label} 투입 공수 (${data.from} ~ ${data.to})`}</h2>
              <CopyButton text={data.text} label="결과 복사" primary />
            </div>
            <ul className="divide-y divide-line text-[14px]">
              {data.months.map((m) => (
                <li key={m.month} className="flex justify-between px-4 py-2.5">
                  <span>{monthLabel(m.month, data.from.slice(0, 4) === data.to.slice(0, 4))}</span>
                  <span className={m.md === 0 ? 'text-mute' : 'font-medium'}>{formatMd(m.md)}</span>
                </li>
              ))}
              <li className="flex justify-between bg-canvas/60 px-4 py-3 font-semibold">
                <span>합계</span>
                <span>{formatMd(data.total)}</span>
              </li>
            </ul>
          </Card>
        </section>
      )}
    </div>
  )
}
