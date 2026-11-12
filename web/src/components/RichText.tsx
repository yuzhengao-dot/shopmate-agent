import type { ReactNode } from 'react';

function inline(text: string, key: string): ReactNode[] {
  const nodes: ReactNode[] = [];
  const regex = /\*\*(.+?)\*\*|`([^`]+)`/g;
  let last = 0;
  let matched: RegExpExecArray | null;
  let index = 0;

  while ((matched = regex.exec(text)) !== null) {
    if (matched.index > last) nodes.push(text.slice(last, matched.index));
    if (matched[1]) {
      nodes.push(
        <strong key={`${key}-b${index}`} className="font-semibold text-ink-800">
          {matched[1]}
        </strong>,
      );
    } else if (matched[2]) {
      nodes.push(
        <code key={`${key}-c${index}`} className="rounded bg-ink-100 px-1 py-0.5 font-mono text-[13px] text-ink-700">
          {matched[2]}
        </code>,
      );
    }
    last = matched.index + matched[0].length;
    index += 1;
  }
  if (last < text.length) nodes.push(text.slice(last));
  return nodes;
}

/** 轻量 Markdown：段落、有序/无序列表、加粗、行内代码 */
export function RichText({ text }: { text: string }) {
  const lines = text.split('\n');
  return (
    <div className="text-[15px] leading-7 text-ink-700">
      {lines.map((line, index) => {
        const key = `line-${index}`;
        if (!line.trim()) return <div key={key} className="h-2" />;
        const ordered = /^(\d+)[.、]\s+/.exec(line);
        const bullet = /^[-•]\s+/.exec(line);
        if (ordered || bullet) {
          const content = line.replace(/^(\d+[.、]\s+|\s*[-•]\s+)/, '');
          return (
            <div key={key} className="flex gap-2 pl-1">
              <span className="shrink-0 text-ink-400 tabular-nums">{ordered ? `${ordered[1]}.` : '·'}</span>
              <span>{inline(content, key)}</span>
            </div>
          );
        }
        return <p key={key}>{inline(line, key)}</p>;
      })}
    </div>
  );
}