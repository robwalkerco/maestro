import { FormattedFlow } from './models';
import React, { useState } from 'react';
import AutosizingTextArea from './AutosizingTextArea';
import { saveAs } from 'file-saver';
import { Modal } from './Modal';

export const SaveFlowModal = ({formattedFlow, onClose}: {
  formattedFlow: FormattedFlow
  onClose: () => void
}) => {
  const [config, setConfig] = useState(formattedFlow.config)
  const [commands, setCommands] = useState(formattedFlow.commands)
  const onSave = () => {
    const content = `${config}\n---\n${commands}`
    saveAs(new Blob([content], {type: 'text/yaml;charset=utf-8'}), "Flow.yaml")
    onClose()
  }
  return (
    <Modal onClose={onClose}>
      <div className="flex flex-col gap-8 p-10 h-full">
        <span
          className="text-lg font-bold"
        >
            Save Flow to File
        </span>
        <div
          className="flex flex-col h-full border rounded bg-red-100"
        >
          <AutosizingTextArea
            className="resize-none p-4 pr-16 overflow-y-scroll overflow-hidden bg-gray-50 font-mono cursor-text outline-none border border-transparent border-b-slate-200 focus:border focus:border-slate-400"
            value={config}
            setValue={setConfig}
          />
          <textarea
            className="resize-none p-4 pr-16 h-full overflow-y-scroll overflow-hidden bg-gray-50 font-mono cursor-text outline-none border border-transparent focus:border focus:border-slate-400"
            value={commands}
            onChange={e => setCommands(e.currentTarget.value)}
          />
        </div>
        <div
          className="flex justify-end gap-2"
        >
          <button
            className="px-4 py-1 border rounded cursor-default hover:bg-slate-100 active:bg-slate-200"
            onClick={onClose}
          >
            Cancel
          </button>
          <button
            className="px-4 py-1 border bg-blue-700 text-white rounded cursor-default hover:bg-blue-800 active:bg-blue-900"
            onClick={onSave}
          >
            Save
          </button>
        </div>
      </div>
    </Modal>
  )
}