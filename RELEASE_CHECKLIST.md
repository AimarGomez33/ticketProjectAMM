# Release & Testing Checklist (Android POS)

Usa esta lista ANTES de entregar una version a cliente.
Regla: si un punto critico falla, no se publica.

## 1) Build y calidad estatica (bloqueante)

- [ ] `./gradlew.bat :app:assembleDebug` termina en SUCCESS.
- [ ] `./gradlew.bat build` termina en SUCCESS.
- [ ] No hay errores en `lint-results-debug.html`.
- [ ] Manifest y Activities nuevas registradas correctamente.
- [ ] Sin errores en archivos tocados (IDE Problems = 0 en cambios).

## 2) Smoke test funcional (bloqueante)

- [ ] La app abre sin crash en arranque.
- [ ] Se puede agregar/quitar cantidades en todas las categorias base.
- [ ] Total y resumen se actualizan al instante.
- [ ] Flujo de impresion no rompe (58mm y 80mm segun aplique).
- [ ] Guardar pedido funciona y aparece en historial admin.
- [ ] Reapertura de app conserva datos esperados.

## 3) Admin panel (bloqueante)

- [ ] Historial visible al abrir admin (sin pulsar botones de ganancia).
- [ ] Ganancia diaria/semanal/mensual funciona sin afectar historial.
- [ ] Boton de editar catalogo abre correctamente.

## 4) Editor de catalogo (bloqueante)

- [ ] Cambiar precio de producto base se refleja en pantalla principal.
- [ ] Cambiar categoria de producto base se refleja tras volver.
- [ ] Reorden de categorias se aplica al regresar.
- [ ] Agregar producto nuevo funciona y queda persistido.
- [ ] El color base (gris) de la card se conserva al agregar platillo nuevo.
- [ ] Productos de hamburguesas NO permiten cambio de nombre.
- [ ] Guardar no permite nombre vacio, precio invalido o duplicados.

## 5) Regresiones criticas (bloqueante)

- [ ] No desaparecen productos existentes tras editar catalogo.
- [ ] No se duplican productos al reabrir categorias.
- [ ] No se rompe el calculo de combos/hamburguesas.
- [ ] No se rompe ticket impreso (nombres, precios, total).

## 6) Compatibilidad minima de dispositivo

- [ ] Probar en 1 telefono real (uso normal).
- [ ] Probar rotacion o layout alterno si aplica.
- [ ] Verificar permisos Bluetooth/red segun version Android.

## 7) Evidencia de salida (obligatoria)

- [ ] Captura de build SUCCESS.
- [ ] Captura de historial admin visible.
- [ ] Captura de editor guardando cambios.
- [ ] Captura de ticket generado.
- [ ] Nota corta de pruebas realizadas (fecha, version, tester).

## 8) Criterio go/no-go

Publicar SOLO si:

- [ ] 100% de checks bloqueantes en verde.
- [ ] 0 errores de build/lint bloqueantes.
- [ ] Evidencias adjuntas.

Si falla cualquier bloqueante: NO publicar.

## 9) Plantilla rapida de reporte

Version: ________
Fecha: ________
Tester: ________
Dispositivo: ________

Resultado:
- Build: PASS/FAIL
- Smoke: PASS/FAIL
- Admin: PASS/FAIL
- Catalogo: PASS/FAIL
- Regresiones: PASS/FAIL

Decision final: GO / NO-GO
Notas: ________________________________________
