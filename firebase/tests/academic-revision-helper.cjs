'use strict';
// Test transport only: append the same revision write required of every Android writer.
// The revision contract suite deliberately bypasses this helper for rejection cases.
exports.withRevision = async function(endpoint, root, headers, user, writes) {
  if (!user || user === 'owner') return writes;
  const touched = writes.some(w => /\/estudiantes\/[^/]+\/(estados|configuracion_academica|operaciones_historial)\//
    .test(w.update?.name || w.delete || ''));
  if (!touched) return writes;
  const name = `${root}/estudiantes/${user}/control_academico/revision`;
  const response = await fetch(`${endpoint}/estudiantes/${user}/control_academico/revision`, { headers: headers(user) });
  const data = await response.json();
  const previous = response.status === 200 ? Number(data.fields.valor.integerValue) : 0;
  return [...writes, { update: { name, fields: {
    version_formato: { integerValue: '1' }, valor: { integerValue: String(previous + 1) }
  } } }];
};
