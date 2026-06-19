/* @ds-bundle: {"format":3,"namespace":"RangeworkDesignSystem_9d40db","components":[{"name":"Button","sourcePath":"components/buttons/Button.jsx"},{"name":"Fab","sourcePath":"components/buttons/Fab.jsx"},{"name":"Card","sourcePath":"components/cards/Card.jsx"},{"name":"ListEntryCard","sourcePath":"components/cards/ListEntryCard.jsx"},{"name":"StatCard","sourcePath":"components/cards/StatCard.jsx"},{"name":"Chip","sourcePath":"components/data-display/Chip.jsx"},{"name":"ClubChip","sourcePath":"components/data-display/Chip.jsx"},{"name":"BallCountPill","sourcePath":"components/data-display/Chip.jsx"},{"name":"NumberBadge","sourcePath":"components/data-display/NumberBadge.jsx"},{"name":"CountStepper","sourcePath":"components/forms/CountStepper.jsx"},{"name":"TextField","sourcePath":"components/forms/TextField.jsx"}],"sourceHashes":{"components/buttons/Button.jsx":"e18c0ea8b3d6","components/buttons/Fab.jsx":"01b23bbc7ecd","components/cards/Card.jsx":"0599fe4d1fdd","components/cards/ListEntryCard.jsx":"ef84c16df234","components/cards/StatCard.jsx":"41bd842001c3","components/data-display/Chip.jsx":"d21923925d99","components/data-display/NumberBadge.jsx":"75cfb1ff5084","components/forms/CountStepper.jsx":"e3de774a6568","components/forms/TextField.jsx":"ceecf17ddcbc","ui_kits/app/App.jsx":"d819163817ad","ui_kits/app/ScreensMain.jsx":"c67bb7d88702","ui_kits/app/ScreensPlanner.jsx":"f61f071f3187","ui_kits/app/data.js":"ddefab1b1690"},"inlinedExternals":[],"unexposedExports":[]} */

(() => {

const __ds_ns = (window.RangeworkDesignSystem_9d40db = window.RangeworkDesignSystem_9d40db || {});

const __ds_scope = {};

(__ds_ns.__errors = __ds_ns.__errors || []);

// components/buttons/Button.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * Rangework button — Material 3 pill button in four variants.
 * Filled (Deep Fairway) for the single primary action; tonal/outlined/text for
 * secondary emphasis. Full-radius shape, DM Sans label-large.
 */
function Button({
  variant = "filled",
  size = "medium",
  disabled = false,
  leadingIcon = null,
  fullWidth = false,
  children,
  style = {},
  ...rest
}) {
  const heights = {
    small: 32,
    medium: 40,
    large: 48
  };
  const padX = {
    small: 16,
    medium: 24,
    large: 28
  };
  const height = heights[size] || 40;
  const palette = {
    filled: {
      background: "var(--primary)",
      color: "var(--on-primary)",
      border: "none"
    },
    tonal: {
      background: "var(--secondary-container)",
      color: "var(--on-secondary-container)",
      border: "none"
    },
    outlined: {
      background: "transparent",
      color: "var(--primary)",
      border: "1px solid var(--outline)"
    },
    text: {
      background: "transparent",
      color: "var(--primary)",
      border: "none"
    }
  };
  const p = palette[variant] || palette.filled;
  const base = {
    display: "inline-flex",
    alignItems: "center",
    justifyContent: "center",
    gap: 8,
    height,
    width: fullWidth ? "100%" : "auto",
    padding: `0 ${padX[size] || 24}px`,
    border: p.border,
    borderRadius: "var(--radius-full)",
    background: p.background,
    color: p.color,
    fontFamily: "var(--font-sans)",
    fontWeight: "var(--label-large-weight)",
    fontSize: "var(--label-large-size)",
    lineHeight: "var(--label-large-line)",
    letterSpacing: "var(--label-large-spacing)",
    cursor: disabled ? "not-allowed" : "pointer",
    opacity: disabled ? 0.38 : 1,
    transition: "filter var(--duration-short) var(--ease-standard), background var(--duration-short) var(--ease-standard)",
    whiteSpace: "nowrap",
    userSelect: "none",
    ...style
  };
  return /*#__PURE__*/React.createElement("button", _extends({
    type: "button",
    disabled: disabled,
    style: base,
    onMouseEnter: e => {
      if (!disabled) e.currentTarget.style.filter = variant === "text" || variant === "outlined" ? "none" : "brightness(0.94)";
      if (!disabled && (variant === "text" || variant === "outlined")) e.currentTarget.style.background = "color-mix(in srgb, var(--primary) 8%, transparent)";
    },
    onMouseLeave: e => {
      e.currentTarget.style.filter = "none";
      if (variant === "text" || variant === "outlined") e.currentTarget.style.background = "transparent";
    }
  }, rest), leadingIcon ? /*#__PURE__*/React.createElement("span", {
    style: {
      display: "inline-flex",
      width: 18,
      height: 18
    }
  }, leadingIcon) : null, children);
}
Object.assign(__ds_scope, { Button });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/buttons/Button.jsx", error: String((e && e.message) || e) }); }

// components/buttons/Fab.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * Rangework floating action button. Primary-container fill (Deep Fairway wash).
 * Use the compact form on list screens; `extended` adds a text label for empty
 * states or prominent calls to action. Defaults to a plus glyph.
 */
function Fab({
  extended = false,
  label = "",
  icon = null,
  style = {},
  ...rest
}) {
  const plus = /*#__PURE__*/React.createElement("svg", {
    width: "22",
    height: "22",
    viewBox: "0 0 24 24",
    fill: "none",
    "aria-hidden": "true"
  }, /*#__PURE__*/React.createElement("path", {
    d: "M12 5v14M5 12h14",
    stroke: "currentColor",
    strokeWidth: "2",
    strokeLinecap: "round"
  }));
  const base = {
    display: "inline-flex",
    alignItems: "center",
    justifyContent: "center",
    gap: extended ? 10 : 0,
    height: 56,
    minWidth: 56,
    width: extended ? "auto" : 56,
    padding: extended ? "0 20px" : 0,
    border: "none",
    borderRadius: "var(--radius-lg)",
    background: "var(--primary-container)",
    color: "var(--on-primary-container)",
    boxShadow: "var(--elevation-3)",
    fontFamily: "var(--font-sans)",
    fontWeight: "var(--label-large-weight)",
    fontSize: "var(--label-large-size)",
    cursor: "pointer",
    transition: "filter var(--duration-short) var(--ease-standard)",
    ...style
  };
  return /*#__PURE__*/React.createElement("button", _extends({
    type: "button",
    style: base,
    onMouseEnter: e => e.currentTarget.style.filter = "brightness(0.96)",
    onMouseLeave: e => e.currentTarget.style.filter = "none"
  }, rest), /*#__PURE__*/React.createElement("span", {
    style: {
      display: "inline-flex"
    }
  }, icon || plus), extended && label ? /*#__PURE__*/React.createElement("span", null, label) : null);
}
Object.assign(__ds_scope, { Fab });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/buttons/Fab.jsx", error: String((e && e.message) || e) }); }

// components/cards/Card.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * Rangework surface card. `outlined` (default) is the workhorse list card —
 * hairline outline, no shadow. `filled` uses a tonal surface. `accent` paints
 * the primary-container wash used for the "Next move" highlight. Optional
 * `onClick` makes the whole card a button with a subtle hover.
 */
function Card({
  variant = "outlined",
  onClick,
  children,
  style = {},
  ...rest
}) {
  const looks = {
    outlined: {
      background: "var(--surface)",
      border: "1px solid var(--outline-variant)",
      color: "var(--on-surface)"
    },
    filled: {
      background: "var(--surface-c-high)",
      border: "none",
      color: "var(--on-surface)"
    },
    accent: {
      background: "var(--primary-container)",
      border: "none",
      color: "var(--on-primary-container)"
    }
  };
  const l = looks[variant] || looks.outlined;
  const interactive = typeof onClick === "function";
  const base = {
    display: "block",
    width: "100%",
    boxSizing: "border-box",
    textAlign: "left",
    background: l.background,
    border: l.border,
    color: l.color,
    borderRadius: "var(--radius-md)",
    padding: "var(--space-4)",
    cursor: interactive ? "pointer" : "default",
    transition: "filter var(--duration-short) var(--ease-standard)",
    ...style
  };
  const hover = interactive ? {
    onMouseEnter: e => e.currentTarget.style.filter = "brightness(0.985)",
    onMouseLeave: e => e.currentTarget.style.filter = "none"
  } : {};
  if (interactive) {
    return /*#__PURE__*/React.createElement("button", _extends({
      type: "button",
      onClick: onClick,
      style: base
    }, hover, rest), children);
  }
  return /*#__PURE__*/React.createElement("div", _extends({
    style: base
  }, rest), children);
}
Object.assign(__ds_scope, { Card });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/cards/Card.jsx", error: String((e && e.message) || e) }); }

// components/cards/ListEntryCard.jsx
try { (() => {
/**
 * List row card for a practice unit or session. Title, an optional metadata row
 * (chips/pills), supporting text, and a trailing overflow menu with
 * Edit / Duplicate / Delete. Mirrors the planner's ListEntryCard.
 */
function ListEntryCard({
  title,
  supportingText = "",
  metadata = null,
  onClick,
  onEdit,
  onDuplicate,
  onDelete,
  style = {}
}) {
  const [open, setOpen] = React.useState(false);
  const wrapRef = React.useRef(null);
  React.useEffect(() => {
    if (!open) return;
    const close = e => {
      if (wrapRef.current && !wrapRef.current.contains(e.target)) setOpen(false);
    };
    document.addEventListener("mousedown", close);
    return () => document.removeEventListener("mousedown", close);
  }, [open]);
  const menuItem = (label, fn, danger) => /*#__PURE__*/React.createElement("button", {
    type: "button",
    onClick: () => {
      setOpen(false);
      fn && fn();
    },
    style: {
      display: "block",
      width: "100%",
      textAlign: "left",
      padding: "10px 16px",
      border: "none",
      background: "transparent",
      cursor: "pointer",
      fontFamily: "var(--font-sans)",
      fontSize: "var(--body-medium-size)",
      color: danger ? "var(--error)" : "var(--on-surface)"
    },
    onMouseEnter: e => e.currentTarget.style.background = "var(--surface-c)",
    onMouseLeave: e => e.currentTarget.style.background = "transparent"
  }, label);
  return /*#__PURE__*/React.createElement(__ds_scope.Card, {
    variant: "outlined",
    style: {
      padding: 0,
      ...style
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "flex-start",
      padding: "12px 4px 12px 16px"
    }
  }, /*#__PURE__*/React.createElement("button", {
    type: "button",
    onClick: onClick,
    style: {
      flex: 1,
      minWidth: 0,
      textAlign: "left",
      border: "none",
      background: "transparent",
      cursor: onClick ? "pointer" : "default",
      padding: 0,
      display: "flex",
      flexDirection: "column",
      gap: 4
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      fontFamily: "var(--font-sans)",
      fontWeight: "var(--title-medium-weight)",
      fontSize: "var(--title-medium-size)",
      lineHeight: "var(--title-medium-line)",
      color: "var(--on-surface)",
      whiteSpace: "nowrap",
      overflow: "hidden",
      textOverflow: "ellipsis"
    }
  }, title), metadata ? /*#__PURE__*/React.createElement("span", {
    style: {
      display: "flex",
      gap: 8,
      flexWrap: "wrap",
      margin: "2px 0"
    }
  }, metadata) : null, supportingText ? /*#__PURE__*/React.createElement("span", {
    style: {
      fontFamily: "var(--font-sans)",
      fontSize: "var(--body-small-size)",
      lineHeight: "var(--body-small-line)",
      letterSpacing: "var(--body-small-spacing)",
      color: "var(--on-surface-variant)"
    }
  }, supportingText) : null), /*#__PURE__*/React.createElement("div", {
    ref: wrapRef,
    style: {
      position: "relative",
      flex: "none"
    }
  }, /*#__PURE__*/React.createElement("button", {
    type: "button",
    "aria-label": "More options",
    onClick: () => setOpen(v => !v),
    style: {
      width: 40,
      height: 40,
      borderRadius: "var(--radius-full)",
      border: "none",
      background: "transparent",
      cursor: "pointer",
      color: "var(--on-surface-variant)",
      display: "inline-flex",
      alignItems: "center",
      justifyContent: "center"
    },
    onMouseEnter: e => e.currentTarget.style.background = "var(--surface-c)",
    onMouseLeave: e => e.currentTarget.style.background = "transparent"
  }, /*#__PURE__*/React.createElement("svg", {
    width: "20",
    height: "20",
    viewBox: "0 0 24 24",
    fill: "currentColor"
  }, /*#__PURE__*/React.createElement("circle", {
    cx: "12",
    cy: "5",
    r: "1.8"
  }), /*#__PURE__*/React.createElement("circle", {
    cx: "12",
    cy: "12",
    r: "1.8"
  }), /*#__PURE__*/React.createElement("circle", {
    cx: "12",
    cy: "19",
    r: "1.8"
  }))), open ? /*#__PURE__*/React.createElement("div", {
    style: {
      position: "absolute",
      top: 44,
      right: 0,
      zIndex: 10,
      minWidth: 160,
      background: "var(--surface-c-low)",
      border: "1px solid var(--outline-variant)",
      borderRadius: "var(--radius-xs)",
      boxShadow: "var(--elevation-2)",
      padding: "4px 0",
      overflow: "hidden"
    }
  }, onEdit ? menuItem("Edit", onEdit) : null, onDuplicate ? menuItem("Duplicate", onDuplicate) : null, onDelete ? menuItem("Delete", onDelete, true) : null) : null)));
}
Object.assign(__ds_scope, { ListEntryCard });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/cards/ListEntryCard.jsx", error: String((e && e.message) || e) }); }

// components/cards/StatCard.jsx
try { (() => {
/**
 * Compact overview metric card: a large DM Mono count over an uppercase label,
 * with a trailing chevron. The count uses the secondary (sage) colour, matching
 * the planner's overview StatCard.
 */
function StatCard({
  label,
  count,
  onClick,
  style = {}
}) {
  return /*#__PURE__*/React.createElement(__ds_scope.Card, {
    variant: "outlined",
    onClick: onClick,
    style: {
      ...style
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 12
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      display: "flex",
      flexDirection: "column",
      gap: 2
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      fontFamily: "var(--font-mono)",
      fontWeight: "var(--mono-large-weight)",
      fontSize: "var(--mono-large-size)",
      lineHeight: "var(--mono-large-line)",
      letterSpacing: "var(--mono-large-spacing)",
      color: "var(--secondary)"
    }
  }, count), /*#__PURE__*/React.createElement("span", {
    style: {
      fontFamily: "var(--font-sans)",
      fontWeight: "var(--label-small-weight)",
      fontSize: "var(--label-small-size)",
      letterSpacing: "var(--label-small-spacing)",
      textTransform: "uppercase",
      color: "var(--on-surface-variant)"
    }
  }, label)), /*#__PURE__*/React.createElement("svg", {
    width: "20",
    height: "20",
    viewBox: "0 0 24 24",
    fill: "none",
    style: {
      color: "var(--on-surface-variant)"
    }
  }, /*#__PURE__*/React.createElement("path", {
    d: "M9 6l6 6-6 6",
    stroke: "currentColor",
    strokeWidth: "2",
    strokeLinecap: "round",
    strokeLinejoin: "round"
  }))));
}
Object.assign(__ds_scope, { StatCard });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/cards/StatCard.jsx", error: String((e && e.message) || e) }); }

// components/data-display/Chip.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * Rangework chip / pill. Two looks:
 *  - "pill" (default): soft surface-container fill, fully rounded. Use for
 *    metadata like ball counts and club names.
 *  - "assist": outlined, tappable affordance (e.g. a "Unit"/"Session" tag).
 * Set `mono` when the content is a numeric metric so it renders in DM Mono.
 */
function Chip({
  variant = "pill",
  mono = false,
  icon = null,
  children,
  style = {},
  ...rest
}) {
  const looks = {
    pill: {
      background: "var(--surface-c-high)",
      color: "var(--on-surface)",
      border: "none"
    },
    assist: {
      background: "transparent",
      color: "var(--on-surface)",
      border: "1px solid var(--outline-variant)"
    }
  };
  const l = looks[variant] || looks.pill;
  const base = {
    display: "inline-flex",
    alignItems: "center",
    gap: 6,
    height: variant === "assist" ? 32 : "auto",
    padding: variant === "assist" ? "0 12px" : "6px 12px",
    borderRadius: "var(--radius-lg)",
    background: l.background,
    color: l.color,
    border: l.border,
    fontFamily: mono ? "var(--font-mono)" : "var(--font-sans)",
    fontWeight: mono ? "var(--mono-small-weight)" : "var(--label-medium-weight)",
    fontSize: mono ? "var(--mono-small-size)" : "var(--label-medium-size)",
    letterSpacing: mono ? 0 : "var(--label-medium-spacing)",
    whiteSpace: "nowrap",
    ...style
  };
  return /*#__PURE__*/React.createElement("span", _extends({
    style: base
  }, rest), icon ? /*#__PURE__*/React.createElement("span", {
    style: {
      display: "inline-flex",
      width: 16,
      height: 16
    }
  }, icon) : null, children);
}
const golfIcon = /*#__PURE__*/React.createElement("svg", {
  width: "16",
  height: "16",
  viewBox: "0 0 24 24",
  fill: "none",
  "aria-hidden": "true"
}, /*#__PURE__*/React.createElement("path", {
  d: "M11 3v13",
  stroke: "currentColor",
  strokeWidth: "1.6",
  strokeLinecap: "round"
}), /*#__PURE__*/React.createElement("path", {
  d: "M11 4l6 2.5-6 2.5",
  stroke: "currentColor",
  strokeWidth: "1.6",
  strokeLinejoin: "round",
  fill: "currentColor",
  fillOpacity: "0.15"
}), /*#__PURE__*/React.createElement("path", {
  d: "M7.5 20h7",
  stroke: "currentColor",
  strokeWidth: "1.6",
  strokeLinecap: "round"
}), /*#__PURE__*/React.createElement("circle", {
  cx: "11",
  cy: "17.5",
  r: "1.4",
  fill: "currentColor"
}));

/** Convenience: a club chip with the golf glyph. */
function ClubChip({
  name,
  ...rest
}) {
  return /*#__PURE__*/React.createElement(Chip, _extends({
    variant: "pill",
    icon: golfIcon
  }, rest), name);
}

/** Convenience: a mono ball-count pill, e.g. "40 balls". */
function BallCountPill({
  count,
  ...rest
}) {
  return /*#__PURE__*/React.createElement(Chip, _extends({
    variant: "pill",
    mono: true
  }, rest), count, " balls");
}
Object.assign(__ds_scope, { Chip, ClubChip, BallCountPill });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/data-display/Chip.jsx", error: String((e && e.message) || e) }); }

// components/data-display/NumberBadge.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * Circular step/position badge. Secondary-container fill, DM Mono numeral.
 * Used to number ordered instructions inside a unit.
 */
function NumberBadge({
  number,
  size = 28,
  style = {},
  ...rest
}) {
  return /*#__PURE__*/React.createElement("span", _extends({
    style: {
      display: "inline-flex",
      alignItems: "center",
      justifyContent: "center",
      width: size,
      height: size,
      borderRadius: "var(--radius-full)",
      background: "var(--secondary-container)",
      color: "var(--on-secondary-container)",
      fontFamily: "var(--font-mono)",
      fontWeight: "var(--mono-small-weight)",
      fontSize: "var(--mono-small-size)",
      flex: "none",
      ...style
    }
  }, rest), number);
}
Object.assign(__ds_scope, { NumberBadge });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/data-display/NumberBadge.jsx", error: String((e && e.message) || e) }); }

// components/forms/CountStepper.jsx
try { (() => {
/**
 * Bounded − / value / + integer stepper. The value renders in DM Mono.
 * Buttons disable at the min/max bounds. 48px touch targets per spec.
 * Used for ball counts and rep counts throughout the planner.
 */
function CountStepper({
  value,
  onValueChange,
  min = 0,
  max = 999,
  label = "Count",
  style = {}
}) {
  const canDec = value > min;
  const canInc = value < max;
  const btn = enabled => ({
    display: "inline-flex",
    alignItems: "center",
    justifyContent: "center",
    width: 48,
    height: 48,
    borderRadius: "var(--radius-full)",
    border: "none",
    background: "var(--surface-c-high)",
    color: "var(--on-surface)",
    cursor: enabled ? "pointer" : "not-allowed",
    opacity: enabled ? 1 : 0.38,
    transition: "filter var(--duration-short) var(--ease-standard)"
  });
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: "inline-flex",
      alignItems: "center",
      gap: 8,
      ...style
    }
  }, /*#__PURE__*/React.createElement("button", {
    type: "button",
    "aria-label": `Decrease ${label}`,
    disabled: !canDec,
    style: btn(canDec),
    onClick: () => canDec && onValueChange(value - 1)
  }, /*#__PURE__*/React.createElement("svg", {
    width: "20",
    height: "20",
    viewBox: "0 0 24 24",
    fill: "none"
  }, /*#__PURE__*/React.createElement("path", {
    d: "M5 12h14",
    stroke: "currentColor",
    strokeWidth: "2",
    strokeLinecap: "round"
  }))), /*#__PURE__*/React.createElement("span", {
    "aria-label": `${label}: ${value}`,
    style: {
      minWidth: 36,
      textAlign: "center",
      fontFamily: "var(--font-mono)",
      fontWeight: "var(--mono-medium-weight)",
      fontSize: "var(--mono-medium-size)",
      lineHeight: "var(--mono-medium-line)",
      color: "var(--on-surface)"
    }
  }, value), /*#__PURE__*/React.createElement("button", {
    type: "button",
    "aria-label": `Increase ${label}`,
    disabled: !canInc,
    style: btn(canInc),
    onClick: () => canInc && onValueChange(value + 1)
  }, /*#__PURE__*/React.createElement("svg", {
    width: "20",
    height: "20",
    viewBox: "0 0 24 24",
    fill: "none"
  }, /*#__PURE__*/React.createElement("path", {
    d: "M12 5v14M5 12h14",
    stroke: "currentColor",
    strokeWidth: "2",
    strokeLinecap: "round"
  }))));
}
Object.assign(__ds_scope, { CountStepper });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/forms/CountStepper.jsx", error: String((e && e.message) || e) }); }

// components/forms/TextField.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * Material 3 outlined text field for Rangework. Floating-style label sits above
 * the input. Supports single-line and multi-line (`rows`), plus an error state
 * with helper text. Body-large content per the typography spec.
 */
function TextField({
  label = "",
  value,
  onChange,
  placeholder = "",
  rows = 1,
  error = false,
  helperText = "",
  disabled = false,
  style = {},
  ...rest
}) {
  const [focused, setFocused] = React.useState(false);
  const multiline = rows > 1;
  const borderColor = error ? "var(--error)" : focused ? "var(--primary)" : "var(--outline)";
  const fieldStyle = {
    width: "100%",
    boxSizing: "border-box",
    padding: "12px 16px",
    border: `${focused ? 2 : 1}px solid ${borderColor}`,
    borderRadius: "var(--radius-xs)",
    background: "transparent",
    color: "var(--on-surface)",
    fontFamily: "var(--font-sans)",
    fontWeight: "var(--body-large-weight)",
    fontSize: "var(--body-large-size)",
    lineHeight: "var(--body-large-line)",
    letterSpacing: "var(--body-large-spacing)",
    outline: "none",
    resize: multiline ? "vertical" : "none",
    margin: focused ? 0 : 1
  };
  const InputEl = multiline ? "textarea" : "input";
  return /*#__PURE__*/React.createElement("label", {
    style: {
      display: "block",
      ...style
    }
  }, label ? /*#__PURE__*/React.createElement("span", {
    style: {
      display: "block",
      marginBottom: 6,
      fontFamily: "var(--font-sans)",
      fontWeight: "var(--label-medium-weight)",
      fontSize: "var(--label-medium-size)",
      letterSpacing: "var(--label-medium-spacing)",
      color: error ? "var(--error)" : "var(--on-surface-variant)"
    }
  }, label) : null, /*#__PURE__*/React.createElement(InputEl, _extends({
    value: value,
    onChange: onChange,
    placeholder: placeholder,
    disabled: disabled,
    rows: multiline ? rows : undefined,
    onFocus: () => setFocused(true),
    onBlur: () => setFocused(false),
    style: {
      ...fieldStyle,
      opacity: disabled ? 0.38 : 1
    }
  }, rest)), helperText ? /*#__PURE__*/React.createElement("span", {
    style: {
      display: "block",
      marginTop: 4,
      fontFamily: "var(--font-sans)",
      fontSize: "var(--body-small-size)",
      letterSpacing: "var(--body-small-spacing)",
      color: error ? "var(--error)" : "var(--on-surface-variant)"
    }
  }, helperText) : null);
}
Object.assign(__ds_scope, { TextField });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/forms/TextField.jsx", error: String((e && e.message) || e) }); }

// ui_kits/app/App.jsx
try { (() => {
/* Rangework UI kit — phone shell: top app bar, content router, bottom nav, FAB. */

const RW_TABS = [{
  key: "overview",
  label: "Overview",
  icon: "home",
  title: "Overview"
}, {
  key: "units",
  label: "Units",
  icon: "widgets",
  title: "Practice units"
}, {
  key: "sessions",
  label: "Sessions",
  icon: "event_note",
  title: "Sessions"
}, {
  key: "settings",
  label: "Settings",
  icon: "tune",
  title: "Settings"
}];
function TopAppBar({
  title,
  onBack
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      height: 56,
      flex: "none",
      display: "flex",
      alignItems: "center",
      gap: 4,
      padding: "0 8px 0 4px",
      background: "var(--surface)",
      borderBottom: "1px solid var(--outline-variant)"
    }
  }, onBack ? /*#__PURE__*/React.createElement("button", {
    onClick: onBack,
    "aria-label": "Back",
    style: iconBtn
  }, /*#__PURE__*/React.createElement("span", {
    className: "material-symbols-rounded"
  }, "arrow_back")) : /*#__PURE__*/React.createElement("div", {
    style: {
      width: 8
    }
  }), /*#__PURE__*/React.createElement("div", {
    className: "rw-title-large",
    style: {
      color: "var(--on-surface)",
      paddingLeft: onBack ? 0 : 8,
      flex: 1
    }
  }, title));
}
const iconBtn = {
  width: 44,
  height: 44,
  borderRadius: "var(--radius-full)",
  border: "none",
  background: "transparent",
  cursor: "pointer",
  color: "var(--on-surface)",
  display: "inline-flex",
  alignItems: "center",
  justifyContent: "center"
};
function BottomNav({
  tab,
  onTab
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      height: 72,
      flex: "none",
      display: "flex",
      background: "var(--surface-c)",
      borderTop: "1px solid var(--outline-variant)"
    }
  }, RW_TABS.map(t => {
    const active = t.key === tab;
    return /*#__PURE__*/React.createElement("button", {
      key: t.key,
      onClick: () => onTab(t.key),
      style: {
        flex: 1,
        border: "none",
        background: "transparent",
        cursor: "pointer",
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        justifyContent: "center",
        gap: 4,
        paddingTop: 8
      }
    }, /*#__PURE__*/React.createElement("span", {
      style: {
        display: "inline-flex",
        alignItems: "center",
        justifyContent: "center",
        width: 56,
        height: 30,
        borderRadius: "var(--radius-full)",
        background: active ? "var(--secondary-container)" : "transparent"
      }
    }, /*#__PURE__*/React.createElement("span", {
      className: "material-symbols-rounded",
      style: {
        fontSize: 22,
        color: active ? "var(--on-secondary-container)" : "var(--on-surface-variant)",
        fontVariationSettings: active ? "'FILL' 1" : "'FILL' 0"
      }
    }, t.icon)), /*#__PURE__*/React.createElement("span", {
      className: "rw-label-small",
      style: {
        color: active ? "var(--on-surface)" : "var(--on-surface-variant)"
      }
    }, t.label));
  }));
}
function App() {
  const NS = window.RangeworkDesignSystem_9d40db;
  const {
    Fab
  } = NS;
  const [signedIn, setSignedIn] = React.useState(false);
  const [route, setRoute] = React.useState({
    tab: "overview",
    unitId: null
  });
  const go = next => setRoute(r => ({
    tab: next.tab ?? r.tab,
    unitId: next.unitId ?? null
  }));
  const onTab = tab => setRoute({
    tab,
    unitId: null
  });
  let title = RW_TABS.find(t => t.key === route.tab)?.title || "Rangework";
  let onBack = null;
  if (route.unitId) {
    title = "Unit";
    onBack = () => setRoute(r => ({
      tab: r.tab,
      unitId: null
    }));
  }
  let body;
  if (route.unitId) body = /*#__PURE__*/React.createElement(UnitDetailScreen, {
    unitId: route.unitId
  });else if (route.tab === "overview") body = /*#__PURE__*/React.createElement(OverviewScreen, {
    go: go
  });else if (route.tab === "units") body = /*#__PURE__*/React.createElement(UnitListScreen, {
    go: go
  });else if (route.tab === "sessions") body = /*#__PURE__*/React.createElement(SessionListScreen, {
    go: go
  });else body = /*#__PURE__*/React.createElement(SettingsScreen, {
    onSignOut: () => setSignedIn(false)
  });
  const showFab = signedIn && !route.unitId && (route.tab === "units" || route.tab === "sessions");
  return /*#__PURE__*/React.createElement("div", {
    className: "rw-phone"
  }, /*#__PURE__*/React.createElement("div", {
    className: "rw-statusbar"
  }, /*#__PURE__*/React.createElement("span", null, "9:41"), /*#__PURE__*/React.createElement("span", {
    style: {
      display: "inline-flex",
      gap: 6,
      alignItems: "center"
    }
  }, /*#__PURE__*/React.createElement("span", {
    className: "material-symbols-rounded",
    style: {
      fontSize: 16
    }
  }, "signal_cellular_alt"), /*#__PURE__*/React.createElement("span", {
    className: "material-symbols-rounded",
    style: {
      fontSize: 16
    }
  }, "wifi"), /*#__PURE__*/React.createElement("span", {
    className: "material-symbols-rounded",
    style: {
      fontSize: 16
    }
  }, "battery_full"))), signedIn ? /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(TopAppBar, {
    title: title,
    onBack: onBack
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      overflowY: "auto",
      background: "var(--background)",
      position: "relative"
    }
  }, body, showFab ? /*#__PURE__*/React.createElement("div", {
    style: {
      position: "sticky",
      bottom: 16,
      display: "flex",
      justifyContent: "flex-end",
      padding: "0 16px",
      pointerEvents: "none"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      pointerEvents: "auto"
    }
  }, /*#__PURE__*/React.createElement(Fab, {
    extended: true,
    label: route.tab === "units" ? "New unit" : "New session"
  }))) : null), /*#__PURE__*/React.createElement(BottomNav, {
    tab: route.tab,
    onTab: onTab
  })) : /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      display: "flex",
      flexDirection: "column",
      background: "var(--background)"
    }
  }, /*#__PURE__*/React.createElement(SignInScreen, {
    onSignIn: () => {
      setSignedIn(true);
      setRoute({
        tab: "overview",
        unitId: null
      });
    }
  })));
}
ReactDOM.createRoot(document.getElementById("root")).render(/*#__PURE__*/React.createElement(App, null));
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/app/App.jsx", error: String((e && e.message) || e) }); }

// ui_kits/app/ScreensMain.jsx
try { (() => {
/* Rangework UI kit — auth, overview & settings screens. */

function SignInScreen({
  onSignIn
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      display: "flex",
      flexDirection: "column",
      alignItems: "center",
      justifyContent: "center",
      padding: "0 32px",
      textAlign: "center",
      gap: 8
    }
  }, /*#__PURE__*/React.createElement("img", {
    src: "../../assets/rangework-mark.svg",
    alt: "",
    style: {
      width: 88,
      height: 88,
      marginBottom: 8
    }
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      fontFamily: "var(--font-sans)",
      fontWeight: 500,
      fontSize: 30,
      letterSpacing: "-0.5px",
      color: "var(--on-background)"
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      color: "var(--primary)"
    }
  }, "range"), "work"), /*#__PURE__*/React.createElement("div", {
    className: "rw-body-medium",
    style: {
      color: "var(--on-surface-variant)",
      maxWidth: 280,
      marginTop: 4
    }
  }, "Plan sharper range sessions. Build drills, compose them into repeatable plans."), /*#__PURE__*/React.createElement("button", {
    onClick: onSignIn,
    style: {
      marginTop: 28,
      display: "inline-flex",
      alignItems: "center",
      gap: 12,
      height: 48,
      padding: "0 22px",
      borderRadius: "var(--radius-full)",
      border: "1px solid var(--outline)",
      background: "var(--background)",
      cursor: "pointer",
      fontFamily: "var(--font-sans)",
      fontWeight: 500,
      fontSize: 14,
      color: "var(--on-surface)"
    }
  }, /*#__PURE__*/React.createElement("img", {
    src: "../../assets/ic_google_logo.svg",
    alt: "",
    style: {
      width: 18,
      height: 18
    }
  }), "Sign in with Google"), /*#__PURE__*/React.createElement("div", {
    className: "rw-body-small",
    style: {
      color: "var(--on-surface-variant)",
      marginTop: 18
    }
  }, "Your practice data stays private to your account."));
}
function SectionLabel({
  children
}) {
  return /*#__PURE__*/React.createElement("div", {
    className: "rw-label-medium",
    style: {
      textTransform: "uppercase",
      color: "var(--on-surface-variant)",
      margin: "4px 0"
    }
  }, children);
}
function OverviewScreen({
  go
}) {
  const NS = window.RangeworkDesignSystem_9d40db;
  const {
    StatCard,
    Card,
    Button,
    Chip
  } = NS;
  const units = window.RW_UNITS,
    sessions = window.RW_SESSIONS;
  const unitsById = Object.fromEntries(units.map(u => [u.id, u]));
  const recent = [{
    kind: "Session",
    name: sessions[0].name,
    meta: window.RW_sessionBalls(sessions[0], unitsById) + " balls · 2 units",
    go: () => go({
      tab: "sessions"
    })
  }, {
    kind: "Unit",
    name: units[0].title,
    meta: window.RW_unitBalls(units[0]) + " balls · " + units[0].club,
    go: () => go({
      tab: "units",
      unitId: units[0].id
    })
  }];
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 16,
      padding: 16
    }
  }, /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    className: "rw-headline-small",
    style: {
      color: "var(--on-background)"
    }
  }, "Welcome back, Logan"), /*#__PURE__*/React.createElement("div", {
    className: "rw-body-small",
    style: {
      color: "var(--on-surface-variant)"
    }
  }, "logan@rangework.app")), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 12
    }
  }, /*#__PURE__*/React.createElement(StatCard, {
    label: "Units",
    count: units.length,
    onClick: () => go({
      tab: "units"
    }),
    style: {
      flex: 1
    }
  }), /*#__PURE__*/React.createElement(StatCard, {
    label: "Sessions",
    count: sessions.length,
    onClick: () => go({
      tab: "sessions"
    }),
    style: {
      flex: 1
    }
  })), /*#__PURE__*/React.createElement(Card, {
    variant: "accent"
  }, /*#__PURE__*/React.createElement("div", {
    className: "rw-label-medium",
    style: {
      textTransform: "uppercase",
      color: "var(--on-primary-container)",
      marginBottom: 10
    }
  }, "Next move"), /*#__PURE__*/React.createElement("div", {
    className: "rw-body-medium",
    style: {
      color: "var(--on-primary-container)",
      marginBottom: 14
    }
  }, "Pick a session to run at the range."), /*#__PURE__*/React.createElement(Button, {
    variant: "tonal",
    onClick: () => go({
      tab: "sessions"
    })
  }, "Open most recent")), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement(SectionLabel, null, "Recently used"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 12,
      overflowX: "auto",
      paddingBottom: 4
    }
  }, recent.map((r, i) => /*#__PURE__*/React.createElement(Card, {
    key: i,
    variant: "outlined",
    onClick: r.go,
    style: {
      width: 190,
      flex: "none"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 6,
      alignItems: "flex-start"
    }
  }, /*#__PURE__*/React.createElement(Chip, {
    variant: "assist"
  }, r.kind), /*#__PURE__*/React.createElement("div", {
    className: "rw-title-small",
    style: {
      color: "var(--on-surface)"
    }
  }, r.name), /*#__PURE__*/React.createElement("div", {
    className: "rw-body-small",
    style: {
      color: "var(--on-surface-variant)"
    }
  }, r.meta)))))));
}
function SettingsRow({
  label,
  value,
  control
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      justifyContent: "space-between",
      padding: "14px 16px",
      gap: 12
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      minWidth: 0
    }
  }, /*#__PURE__*/React.createElement("div", {
    className: "rw-body-large",
    style: {
      color: "var(--on-surface)"
    }
  }, label), value ? /*#__PURE__*/React.createElement("div", {
    className: "rw-body-small",
    style: {
      color: "var(--on-surface-variant)"
    }
  }, value) : null), control || null);
}
function Segmented({
  options,
  value,
  onChange
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: "inline-flex",
      border: "1px solid var(--outline)",
      borderRadius: "var(--radius-full)",
      overflow: "hidden"
    }
  }, options.map((o, i) => /*#__PURE__*/React.createElement("button", {
    key: o,
    onClick: () => onChange(o),
    style: {
      padding: "7px 14px",
      border: "none",
      borderLeft: i ? "1px solid var(--outline-variant)" : "none",
      background: value === o ? "var(--secondary-container)" : "transparent",
      color: value === o ? "var(--on-secondary-container)" : "var(--on-surface-variant)",
      fontFamily: "var(--font-sans)",
      fontWeight: 500,
      fontSize: 13,
      cursor: "pointer"
    }
  }, o)));
}
function SettingsScreen({
  onSignOut
}) {
  const NS = window.RangeworkDesignSystem_9d40db;
  const {
    Card
  } = NS;
  const [theme, setTheme] = React.useState("System");
  const [dist, setDist] = React.useState("Yards");
  const [speed, setSpeed] = React.useState("mph");
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 16,
      padding: 16
    }
  }, /*#__PURE__*/React.createElement(SectionLabel, null, "Appearance"), /*#__PURE__*/React.createElement(Card, {
    variant: "outlined",
    style: {
      padding: 0
    }
  }, /*#__PURE__*/React.createElement(SettingsRow, {
    label: "Theme",
    value: "Light, Dark, or follow system",
    control: /*#__PURE__*/React.createElement(Segmented, {
      options: ["Light", "Dark", "System"],
      value: theme,
      onChange: setTheme
    })
  })), /*#__PURE__*/React.createElement(SectionLabel, null, "Units of measure"), /*#__PURE__*/React.createElement(Card, {
    variant: "outlined",
    style: {
      padding: 0
    }
  }, /*#__PURE__*/React.createElement(SettingsRow, {
    label: "Distance",
    control: /*#__PURE__*/React.createElement(Segmented, {
      options: ["Yards", "Metres"],
      value: dist,
      onChange: setDist
    })
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 1,
      background: "var(--outline-variant)",
      margin: "0 16px"
    }
  }), /*#__PURE__*/React.createElement(SettingsRow, {
    label: "Speed",
    control: /*#__PURE__*/React.createElement(Segmented, {
      options: ["mph", "km/h", "m/s"],
      value: speed,
      onChange: setSpeed
    })
  })), /*#__PURE__*/React.createElement(SectionLabel, null, "Equipment"), /*#__PURE__*/React.createElement(Card, {
    variant: "outlined",
    onClick: () => {},
    style: {
      padding: 0
    }
  }, /*#__PURE__*/React.createElement(SettingsRow, {
    label: "Club bag",
    value: "14 of 30 clubs enabled",
    control: /*#__PURE__*/React.createElement("span", {
      className: "material-symbols-rounded",
      style: {
        color: "var(--on-surface-variant)"
      }
    }, "chevron_right")
  })), /*#__PURE__*/React.createElement(SectionLabel, null, "Account"), /*#__PURE__*/React.createElement(Card, {
    variant: "outlined",
    style: {
      padding: 0
    }
  }, /*#__PURE__*/React.createElement(SettingsRow, {
    label: "Signed in",
    value: "logan@rangework.app"
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 1,
      background: "var(--outline-variant)",
      margin: "0 16px"
    }
  }), /*#__PURE__*/React.createElement("button", {
    onClick: onSignOut,
    style: {
      width: "100%",
      textAlign: "left",
      padding: "14px 16px",
      border: "none",
      background: "transparent",
      cursor: "pointer",
      fontFamily: "var(--font-sans)",
      fontSize: 16,
      color: "var(--error)"
    }
  }, "Sign out")));
}
Object.assign(window, {
  SignInScreen,
  OverviewScreen,
  SettingsScreen
});
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/app/ScreensMain.jsx", error: String((e && e.message) || e) }); }

// ui_kits/app/ScreensPlanner.jsx
try { (() => {
/* Rangework UI kit — planner list & detail screens. */

function UnitListScreen({
  go
}) {
  const NS = window.RangeworkDesignSystem_9d40db;
  const {
    ListEntryCard,
    ClubChip,
    BallCountPill
  } = NS;
  const units = window.RW_UNITS;
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 12,
      padding: 16
    }
  }, units.map(u => /*#__PURE__*/React.createElement(ListEntryCard, {
    key: u.id,
    title: u.title,
    metadata: /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(ClubChip, {
      name: u.club
    }), /*#__PURE__*/React.createElement(BallCountPill, {
      count: window.RW_unitBalls(u)
    })),
    supportingText: u.instructions.length + " instructions",
    onClick: () => go({
      tab: "units",
      unitId: u.id
    }),
    onEdit: () => {},
    onDuplicate: () => {},
    onDelete: () => {}
  })));
}
function SessionListScreen({
  go
}) {
  const NS = window.RangeworkDesignSystem_9d40db;
  const {
    ListEntryCard,
    BallCountPill,
    Chip
  } = NS;
  const sessions = window.RW_SESSIONS;
  const unitsById = Object.fromEntries(window.RW_UNITS.map(u => [u.id, u]));
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 12,
      padding: 16
    }
  }, sessions.map(s => {
    const unitNames = s.items.map(it => unitsById[it.unitId]?.title).filter(Boolean).join(" · ");
    const totalReps = s.items.reduce((t, it) => t + it.reps, 0);
    return /*#__PURE__*/React.createElement(ListEntryCard, {
      key: s.id,
      title: s.name,
      metadata: /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(BallCountPill, {
        count: window.RW_sessionBalls(s, unitsById)
      }), /*#__PURE__*/React.createElement(Chip, {
        mono: true
      }, "×" + totalReps + " reps")),
      supportingText: unitNames,
      onClick: () => {},
      onEdit: () => {},
      onDuplicate: () => {},
      onDelete: () => {}
    });
  }));
}
function FocusCard({
  cue
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 12,
      alignItems: "flex-start",
      background: "var(--secondary-container)",
      color: "var(--on-secondary-container)",
      borderRadius: "var(--radius-md)",
      padding: 16
    }
  }, /*#__PURE__*/React.createElement("span", {
    className: "material-symbols-rounded",
    style: {
      fontSize: 22
    }
  }, "my_location"), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    className: "rw-label-medium",
    style: {
      textTransform: "uppercase",
      opacity: 0.85,
      marginBottom: 2
    }
  }, "Focus"), /*#__PURE__*/React.createElement("div", {
    className: "rw-body-medium"
  }, cue)));
}
function BriefingStat({
  value,
  label,
  colored
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      textAlign: "center"
    }
  }, /*#__PURE__*/React.createElement("div", {
    className: "rw-mono-medium",
    style: {
      color: colored ? "var(--secondary)" : "var(--on-surface)"
    }
  }, value), /*#__PURE__*/React.createElement("div", {
    className: "rw-label-small",
    style: {
      textTransform: "uppercase",
      color: "var(--on-surface-variant)",
      marginTop: 2
    }
  }, label));
}
function UnitDetailScreen({
  unitId
}) {
  const NS = window.RangeworkDesignSystem_9d40db;
  const {
    Card,
    NumberBadge,
    BallCountPill
  } = NS;
  const unit = window.RW_UNITS.find(u => u.id === unitId) || window.RW_UNITS[0];
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 16,
      padding: 16
    }
  }, /*#__PURE__*/React.createElement(Card, {
    variant: "outlined"
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex"
    }
  }, /*#__PURE__*/React.createElement(BriefingStat, {
    value: window.RW_unitBalls(unit),
    label: "Balls",
    colored: true
  }), /*#__PURE__*/React.createElement(BriefingStat, {
    value: unit.instructions.length,
    label: "Instructions"
  }), /*#__PURE__*/React.createElement(BriefingStat, {
    value: unit.club,
    label: "Default club"
  }))), unit.notes ? /*#__PURE__*/React.createElement(Card, {
    variant: "filled"
  }, /*#__PURE__*/React.createElement("div", {
    className: "rw-label-medium",
    style: {
      textTransform: "uppercase",
      color: "var(--on-surface-variant)",
      marginBottom: 6
    }
  }, "Unit notes"), /*#__PURE__*/React.createElement("div", {
    className: "rw-body-medium",
    style: {
      color: "var(--on-surface)"
    }
  }, unit.notes)) : null, unit.focus ? /*#__PURE__*/React.createElement(FocusCard, {
    cue: unit.focus
  }) : null, /*#__PURE__*/React.createElement(Card, {
    variant: "outlined"
  }, /*#__PURE__*/React.createElement("div", {
    className: "rw-title-medium",
    style: {
      color: "var(--on-surface)",
      marginBottom: 12
    }
  }, "Instructions"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column"
    }
  }, unit.instructions.map((ins, i) => /*#__PURE__*/React.createElement("div", {
    key: i
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 12,
      alignItems: "flex-start",
      padding: "10px 0"
    }
  }, /*#__PURE__*/React.createElement(NumberBadge, {
    number: i + 1
  }), /*#__PURE__*/React.createElement("div", {
    className: "rw-body-medium",
    style: {
      color: "var(--on-surface)",
      flex: 1
    }
  }, ins.text), ins.balls ? /*#__PURE__*/React.createElement(BallCountPill, {
    count: ins.balls
  }) : null), i < unit.instructions.length - 1 ? /*#__PURE__*/React.createElement("div", {
    style: {
      height: 1,
      background: "var(--outline-variant)"
    }
  }) : null)))));
}
Object.assign(window, {
  UnitListScreen,
  SessionListScreen,
  UnitDetailScreen
});
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/app/ScreensPlanner.jsx", error: String((e && e.message) || e) }); }

// ui_kits/app/data.js
try { (() => {
// Rangework UI kit — fake planner data (mirrors the shared domain models).
window.RW_CLUBS = [{
  code: "7i",
  name: "7 Iron"
}, {
  code: "pw",
  name: "Pitching Wedge"
}, {
  code: "sw",
  name: "Sand Wedge"
}, {
  code: "dr",
  name: "Driver"
}, {
  code: "pt",
  name: "Putter"
}, {
  code: "9i",
  name: "9 Iron"
}];
window.RW_UNITS = [{
  id: "u1",
  title: "50-yard pitch shots",
  club: "Sand Wedge",
  focus: "Soft hands through impact",
  notes: "Pick three landing zones and rotate. Reset tempo between each.",
  instructions: [{
    text: "Land 10 balls on the near flag (40 yds)",
    balls: 10
  }, {
    text: "Land 10 balls on the mid flag (50 yds)",
    balls: 10
  }, {
    text: "Land 10 balls on the far flag (60 yds)",
    balls: 10
  }]
}, {
  id: "u2",
  title: "Iron shot fundamentals",
  club: "7 Iron",
  focus: "Hinge at impact, follow through high",
  notes: "",
  instructions: [{
    text: "Half swings, feel the strike",
    balls: 10
  }, {
    text: "Three-quarter to a target",
    balls: 15
  }, {
    text: "Full swing, hold the finish",
    balls: 15
  }]
}, {
  id: "u3",
  title: "Gate putting drill",
  club: "Putter",
  focus: "Square face at impact",
  notes: "Tees one putter-head apart.",
  instructions: [{
    text: "6-foot gate, both hands",
    balls: 10
  }, {
    text: "10-foot gate, lead hand only",
    balls: 10
  }]
}, {
  id: "u4",
  title: "Driver tempo ladder",
  club: "Driver",
  focus: "Smooth 80% tempo",
  notes: "",
  instructions: [{
    text: "Five swings at 70% effort",
    balls: 5
  }, {
    text: "Five swings at 80% effort",
    balls: 5
  }, {
    text: "Five swings at full commit",
    balls: 5
  }]
}];
window.RW_SESSIONS = [{
  id: "s1",
  name: "Short game tune-up",
  items: [{
    unitId: "u1",
    reps: 2
  }, {
    unitId: "u3",
    reps: 1
  }]
}, {
  id: "s2",
  name: "Full bag warm-up",
  items: [{
    unitId: "u2",
    reps: 1
  }, {
    unitId: "u4",
    reps: 1
  }, {
    unitId: "u1",
    reps: 1
  }]
}];
window.RW_unitBalls = u => u.instructions.reduce((t, i) => t + (i.balls || 0), 0);
window.RW_sessionBalls = (s, unitsById) => s.items.reduce((t, it) => t + (unitsById[it.unitId] ? RW_unitBalls(unitsById[it.unitId]) * it.reps : 0), 0);
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/app/data.js", error: String((e && e.message) || e) }); }

__ds_ns.Button = __ds_scope.Button;

__ds_ns.Fab = __ds_scope.Fab;

__ds_ns.Card = __ds_scope.Card;

__ds_ns.ListEntryCard = __ds_scope.ListEntryCard;

__ds_ns.StatCard = __ds_scope.StatCard;

__ds_ns.Chip = __ds_scope.Chip;

__ds_ns.ClubChip = __ds_scope.ClubChip;

__ds_ns.BallCountPill = __ds_scope.BallCountPill;

__ds_ns.NumberBadge = __ds_scope.NumberBadge;

__ds_ns.CountStepper = __ds_scope.CountStepper;

__ds_ns.TextField = __ds_scope.TextField;

})();
