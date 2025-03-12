import { editor } from 'monaco-editor';

export type StoredModel = { editor: editor.IStandaloneCodeEditor; textModel: editor.ITextModel; viewState: editor.ICodeEditorViewState | null };

class ModelStorage {
  private models: Record<string, StoredModel> = {};

  public updateModel(id: string, model: StoredModel) {
    this.models[id] = model;
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
