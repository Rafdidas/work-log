export const HOURS_PER_MD = 8

export function formatMd(md: number): string {
  return `${md.toFixed(2)}MD`
}

export function mdToHours(md: number): string {
  return `${(md * HOURS_PER_MD).toFixed(1)}h`
}

/**
 * 저장 전 미리보기용. 서버 MdAllocator와 같은 최대 나머지 방식으로 0.01 단위로 나눈다.
 * 합계가 정확히 workMd가 되고, 남는 0.01은 나머지가 큰 항목(동률이면 앞 항목)이 가져간다.
 */
export function previewAllocation(
  workMd: number,
  weights: { id: number; weight: number }[],
): Record<number, number> {
  const totalUnits = Math.round(workMd * 100)
  const weightSum = weights.reduce((sum, w) => sum + w.weight, 0)
  const units: Record<number, number> = {}
  weights.forEach((w) => (units[w.id] = 0))
  if (weightSum > 0 && totalUnits > 0) {
    const remainders: { id: number; remainder: number; order: number }[] = []
    let assigned = 0
    weights.forEach((w, order) => {
      const scaled = totalUnits * w.weight
      const base = Math.floor(scaled / weightSum)
      units[w.id] = base
      assigned += base
      if (w.weight > 0) remainders.push({ id: w.id, remainder: scaled % weightSum, order })
    })
    remainders.sort((a, b) => b.remainder - a.remainder || a.order - b.order)
    for (let i = 0; i < totalUnits - assigned; i++) units[remainders[i].id] += 1
  }
  const result: Record<number, number> = {}
  Object.entries(units).forEach(([id, u]) => (result[Number(id)] = u / 100))
  return result
}
