import { formatMd, mdToHours, previewAllocation } from './md'

describe('md', () => {
  it('formatMd rounds to two decimals', () => {
    expect(formatMd(0.666)).toBe('0.67MD')
    expect(formatMd(1)).toBe('1.00MD')
  })

  it('mdToHours converts at 8 hours per MD', () => {
    expect(mdToHours(0.67)).toBe('5.4h')
    expect(mdToHours(1)).toBe('8.0h')
  })

  it('previewAllocation splits by weight', () => {
    expect(
      previewAllocation(1, [
        { id: 1, weight: 2 },
        { id: 2, weight: 1 },
      ]),
    ).toEqual({ 1: 0.67, 2: 0.33 })
  })

  it('previewAllocation gives the spare 0.01 to the first item on a tie', () => {
    expect(
      previewAllocation(1, [
        { id: 1, weight: 1 },
        { id: 2, weight: 1 },
        { id: 3, weight: 1 },
      ]),
    ).toEqual({ 1: 0.34, 2: 0.33, 3: 0.33 })
  })

  it('previewAllocation halves on a half day and prefers the larger remainder', () => {
    expect(
      previewAllocation(0.5, [
        { id: 1, weight: 2 },
        { id: 2, weight: 1 },
      ]),
    ).toEqual({ 1: 0.33, 2: 0.17 })
  })

  it('previewAllocation gives zero when every weight is zero', () => {
    expect(
      previewAllocation(1, [
        { id: 1, weight: 0 },
        { id: 2, weight: 0 },
      ]),
    ).toEqual({ 1: 0, 2: 0 })
  })
})
