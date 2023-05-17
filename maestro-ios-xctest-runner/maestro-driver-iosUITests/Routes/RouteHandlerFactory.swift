import Foundation
import FlyingFox

class RouteHandlerFactory {
    @MainActor class func createRouteHandler(route: Route) -> HTTPHandler {
        switch route {
        case .subTree:
            return SubTreeRouteHandler()
        case .runningApp:
            return RunningAppRouteHandler()
        case .swipe:
            return SwipeRouteHandler()
        case .swipeV2:
            return SwipeRouteHandlerV2()
        case .inputText:
            return InputTextRouteHandler()
        case .touch:
            return TouchRouteHandler()
        case .screenshot:
            return ScreenshotHandler()
        case .isScreenStatic:
            return IsScreenStaticHandler()
        case .pressKey:
            return PressKeyHandler()
        case .pressButton:
            return PressButtonHandler()
        case .eraseText:
            return EraseTextHandler()
        case .deviceInfo:
            return DeviceInfoHandler()
        case .setPermissions:
            return SetPermissionsHandler()
        case .doubleTap:
            return DoubleTapRouteHandler()
        }
    }
}
