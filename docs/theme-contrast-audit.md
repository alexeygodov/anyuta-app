# Расчёт контраста предлагаемой палитры

Дата: 13.09.2026. Дополнение к [заданию](theme-design-handoff.md).
Это расчёт HEX из документа, а не запуск ThemeContrastTest или рендер приложения.

Для канала sRGB c = byte/255: lin(c) = c/12.92 при c <= 0.04045,
иначе ((c + 0.055)/1.055)^2.4.
L = 0.2126 lin(R) + 0.7152 lin(G) + 0.0722 lin(B).
Контраст = (max(L1,L2)+0.05)/(min(L1,L2)+0.05).
Сравнение с 4.5 выполнено до округления.

Все 86 проверенных пар >= 4.5:1. Прозрачность, анимации, реальные
поверхности Material, линии графиков и виджеты требуют проверки при реализации.

| Текст | Фон | Светлая | Тёмная |
| --- | --- | --- | --- |
| onBackground | background | 13.65 | 14.24 |
| onPrimary | primary | 6.18 | 8.17 |
| onPrimaryContainer | primaryContainer | 9.88 | 7.09 |
| onSecondary | secondary | 6.27 | 7.76 |
| onSecondaryContainer | secondaryContainer | 13.53 | 8.98 |
| onTertiary | tertiary | 5.31 | 8.23 |
| onTertiaryContainer | tertiaryContainer | 9.59 | 9.97 |
| onError | error | 6.46 | 7.72 |
| onErrorContainer | errorContainer | 13.26 | 9.32 |
| inverseOnSurface | inverseSurface | 11.69 | 10.64 |
| inversePrimary | inverseSurface | 8.17 | 4.80 |
| onSurface | background | 13.65 | 14.24 |
| onSurfaceVariant | background | 8.57 | 10.74 |
| onSurface | surface | 14.68 | 13.24 |
| onSurfaceVariant | surface | 9.21 | 9.98 |
| onSurface | surfaceContainerLowest | 14.73 | 14.83 |
| onSurfaceVariant | surfaceContainerLowest | 9.24 | 11.18 |
| onSurface | surfaceContainerLow | 13.24 | 12.97 |
| onSurfaceVariant | surfaceContainerLow | 8.31 | 9.77 |
| onSurface | surfaceContainer | 12.61 | 11.96 |
| onSurfaceVariant | surfaceContainer | 7.91 | 9.02 |
| onSurface | surfaceContainerHigh | 12.02 | 10.64 |
| onSurfaceVariant | surfaceContainerHigh | 7.54 | 8.02 |
| onSurface | surfaceContainerHighest | 11.45 | 9.06 |
| onSurfaceVariant | surfaceContainerHighest | 7.18 | 6.83 |
| onSurface | surfaceBright | 14.68 | 8.73 |
| onSurfaceVariant | surfaceBright | 9.21 | 6.58 |
| onSurface | surfaceDim | 11.27 | 14.24 |
| onSurfaceVariant | surfaceDim | 7.07 | 10.74 |
| onSurface | surfaceVariant | 12.53 | 9.06 |
| onSurfaceVariant | surfaceVariant | 7.86 | 6.83 |
| primary | surface | 6.16 | 10.71 |
| secondary | surface | 6.24 | 10.07 |
| tertiary | surface | 5.29 | 9.99 |
| error | surface | 6.44 | 10.04 |
| primary | surfaceContainerLow | 5.56 | 10.49 |
| secondary | surfaceContainerLow | 5.63 | 9.86 |
| tertiary | surfaceContainerLow | 4.77 | 9.78 |
| error | surfaceContainerLow | 5.81 | 9.83 |
| onSurface | primaryContainer | 12.29 | 7.06 |
| onSurface | secondaryContainer | 11.87 | 9.02 |
| onSurface | tertiaryContainer | 11.71 | 9.75 |
| onSurface | errorContainer | 11.40 | 9.36 |
