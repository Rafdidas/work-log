/** 서버가 만든 주간 보고 텍스트에서 한 섹션의 본문(머리말 줄 제외)을 꺼낸다. 섹션은 빈 줄로 구분된다. */
export function sectionBody(text: string, header: string): string {
  const block = text.split('\n\n').find((b) => b.startsWith(`[${header}]`))
  if (!block) return ''
  return block.split('\n').slice(1).join('\n')
}
