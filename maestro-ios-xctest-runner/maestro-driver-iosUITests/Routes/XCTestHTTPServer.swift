import FlyingFox

enum Route: String, CaseIterable {
    case subTree
    case runningApp
    case swipe
    case swipeV2
    case inputText
    case touch
    case screenshot
    case isScreenStatic
    case pressKey
    case pressButton
    case eraseText
    case deviceInfo
    case setPermissions
    case doubleTap
    case copyText
    
    func toHTTPRoute() -> HTTPRoute {
        return HTTPRoute(rawValue)
    }
}

struct XCTestHTTPServer {
    func start() async throws {
        let server = HTTPServer(address: .loopback(port: 22087))
        
        for route in Route.allCases {
            let handler = await RouteHandlerFactory.createRouteHandler(route: route)
            await server.appendRoute(route.toHTTPRoute(), to: handler)
        }
        
        try await server.start()
    }
}
