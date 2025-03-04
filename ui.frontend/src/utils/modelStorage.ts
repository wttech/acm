import { editor } from 'monaco-editor';

export type StoredModel = { textModel: editor.ITextModel; viewState: editor.ICodeEditorViewState | null };

class ModelStorage {
  private models: Record<string, StoredModel> = {};

  public updateModel(id: string, textModel: editor.ITextModel, viewState: editor.ICodeEditorViewState | null) {
    this.models[id] = { textModel, viewState };
  }

  public updateViewState(id: string, viewState: editor.ICodeEditorViewState | null) {
    this.models[id].viewState = viewState;
  }

  public deleteModel(id: string) {
    delete this.models[id];
  }

  public getModel(id: string): StoredModel | null {
    return this.models[id];
  }
}

export const modelStorage = new ModelStorage();
