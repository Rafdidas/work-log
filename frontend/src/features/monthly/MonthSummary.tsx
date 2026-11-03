import { useState } from 'react'
import { DAY_TYPE_LABEL, type DayType } from '../../api/types'
import { Card } from '../../components/Card'
import { CopyButton } from '../../components/CopyButton'
import { formatMd } from '../../lib/md'
import { todaySeoul } from '../../lib/week'
import { useMonthly } from './useReports'

const LEAVE_TYPES: DayType[] = ['ANNUAL', 'HALF_AM', 'HALF_PM', 'HOLIDAY']

export function MonthSummary() {
  const [month, setMonth] = useState(todaySeoul().slice(0, 7))
  const { data, isError, error } = useMonthly(month)

  return (
    <div>
      <label className="block text-[13px] text-charcoal">
        월 선택
        <input
          type="month"
          value={month}
          onChange={(e) => setMonth(e.target.value)}
          className="mt-1 block h-10 w-44 rounded-xl border border-line bg-white px-3 text-[14px] outline-none focus:border-prussian focus:ring-2 focus:ring-prussian/15"
        />
      </label>

      {isError && <p className="mt-3 text-sm text-red-700">{error.message}</p>}

      {data && (
        <Card className="mt-4">
          <div className="flex items-center justify-between border-b border-line px-4 py-3">
            <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-[13px] text-charcoal">
              <span>{`근무일 ${data.workDays}일`}</span>
              {LEAVE_TYPES.filter((t) => (data.leaveCounts[t] ?? 0) > 0).map((t) => (
                <span key={t}>{`${DAY_TYPE_LABEL[t]} ${data.leaveCounts[t]}`}</span>
              ))}
              <span className="font-semibold text-ink">{`총 ${formatMd(data.totalMd)}`}</span>
            </div>
            <CopyButton text={data.tsv} label="표 복사" primary />
          </div>

          {data.rows.length === 0 ? (
            <p className="px-4 py-6 text-center text-[13px] text-mute">이 달의 기록이 없어요.</p>
          ) : (
            <table className="w-full text-[14px]">
              <thead>
                <tr className="text-left text-[12px] text-mute">
                  <th className="px-4 py-2 font-medium">태그</th>
                  <th className="py-2 font-medium">업무</th>
                  <th className="py-2 text-right font-medium">MD</th>
                  <th className="px-4 py-2 text-right font-medium">비율</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-line">
                {data.rows.map((r) => (
                  <tr key={`${r.tag}-${r.title}`}>
                    <td className="px-4 py-2.5 text-charcoal">{r.tag ?? '-'}</td>
                    <td className="py-2.5">{r.title}</td>
                    <td className="py-2.5 text-right font-medium">{formatMd(r.md)}</td>
                    <td className="px-4 py-2.5 text-right text-charcoal">{`${r.ratioPercent.toFixed(1)}%`}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </Card>
      )}
    </div>
  )
}
