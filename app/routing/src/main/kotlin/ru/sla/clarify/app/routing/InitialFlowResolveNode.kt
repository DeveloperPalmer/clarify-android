package ru.sla.clarify.app.routing

import ru.kode.way.extension.node.hook.BaseScreenNode

// Транзиентный экран-резолвер: решает, какой флоу начальный (логин или conversation), и никогда ничего не рисует.
// Он намеренно НЕ ComposableNode — иначе PredictiveNodeHost.findBelowScreen счёл бы этот пустой узел «экраном
// снизу» корня conversation (chatList) и включил бы там внутренний predictive-жест, украв у ОС анимацию
// back-to-home. См. PREDICTIVE_BACK_ROOT_HOME_BUG.md.
class InitialFlowResolveNode : BaseScreenNode()
