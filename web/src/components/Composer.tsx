interface ComposerProps {
  value: string;
  onChange: (value: string) => void;
  onSend: (text: string) => void;
  onStop: () => void;
  streaming: boolean;
}

export function Composer({ value, onChange, onSend, onStop, streaming }: ComposerProps) {
  const submit = () => {
    if (streaming || !value.trim()) return;
    onSend(value);
  };

  return (
    <div className="border-t border-ink-200 bg-white px-4 py-3 md:px-6">
      <div className="mx-auto flex max-w-3xl items-end gap-2 rounded-lg border border-ink-200 bg-white px-3 py-2 focus-within:border-ink-400">
        <textarea
          value={value}
          rows={2}
          placeholder="问商品、查订单、办售后，例如：预算 3000 元以内，推荐一款拍照好的手机"
          onChange={(event) => onChange(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === 'Enter' && !event.shiftKey) {
              event.preventDefault();
              submit();
            }
          }}
          className="max-h-40 min-h-[44px] flex-1 resize-none bg-transparent py-1 text-[15px] leading-6 text-ink-800 placeholder:text-ink-300 focus:outline-none"
        />
        {streaming ? (
          <button
            type="button"
            onClick={onStop}
            className="mb-0.5 rounded-md border border-ink-300 px-3 py-1.5 text-xs text-ink-600 hover:bg-ink-50"
          >
            停止
          </button>
        ) : (
          <button
            type="button"
            onClick={submit}
            disabled={!value.trim()}
            className="mb-0.5 rounded-md bg-ink-900 px-3.5 py-1.5 text-xs font-medium text-white transition-colors hover:bg-ink-800 disabled:cursor-not-allowed disabled:bg-ink-200"
          >
            发送
          </button>
        )}
      </div>
      <p className="mx-auto mt-1.5 max-w-3xl text-[11px] text-ink-300">
        Enter 发送 · Shift + Enter 换行 · 所有商品与订单数据来自本地 H2 示例库
      </p>
    </div>
  );
}