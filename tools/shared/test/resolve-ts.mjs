// The sources import each other without extensions (bundler style); Node needs `.ts` to run them.
export async function resolve(specifier, context, nextResolve) {
  if (specifier.startsWith('.') && !/\.\w+$/.test(specifier)) {
    return nextResolve(`${specifier}.ts`, context);
  }
  return nextResolve(specifier, context);
}
