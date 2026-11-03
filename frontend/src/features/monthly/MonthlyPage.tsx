import { useState } from 'react'
import { SegmentedTabs } from '../../components/SegmentedTabs'
import { EffortQuery } from './EffortQuery'
import { MonthSummary } from './MonthSummary'

type View = 'effort' | 'summary'

export function MonthlyPage() {
  const [view, setView] = useState<View>('effort')
  return (
    <div>
      <SegmentedTabs
        ariaLabel="월별 보기"
        options={[
          { value: 'effort' as View, label: '공수 조회' },
          { value: 'summary' as View, label: '월간 요약' },
        ]}
        value={view}
        onChange={setView}
      />
      <div className="mt-5">{view === 'effort' ? <EffortQuery /> : <MonthSummary />}</div>
    </div>
  )
}
