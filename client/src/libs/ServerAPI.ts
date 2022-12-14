import { GetToken, HasToken } from "./Token";

interface HandleStatusData {
    isOk: boolean;
    status: number;
    data: any | undefined;
}

interface AnyServerAPIParams {
    url: string;
    urlParams?: Object | undefined;
    body?: any | undefined;
    onDataReceived?: ((data: any) => void) | undefined;
    handleStatus?: ((data: HandleStatusData) => void) | undefined;
    handleServerError?: (() => void) | undefined;
    headers?: HeadersInit | undefined;
    useToken?: boolean | undefined;
}

const DefaultServerAPIParams: AnyServerAPIParams = {
    url: "/",
    urlParams: undefined,
    body: undefined,
    onDataReceived: undefined,
    handleStatus: undefined,
    headers: undefined,
    useToken: undefined,
};

interface ServerAPIParams extends AnyServerAPIParams {
    method: string;
}

const getStrFromParams = (rawParams: Object | undefined) => {
    return rawParams
        ? "?" +
              Object.entries(rawParams)
                  .map((item) => {
                      return `${item[0]}=${item[1]}`;
                  })
                  .join("&")
        : "";
};

const ServerAPI = (params: ServerAPIParams) => {
    let headers = params.headers || {
        "Content-Type": "application/json; charset=UTF-8",
    };
    if (HasToken() && (params.useToken === undefined || params.useToken === true)) {
        headers = params.headers || {
            "Content-Type": "application/json; charset=UTF-8",
            Authorization: "Bearer " + GetToken(),
        };
    }
    fetch(params.url + getStrFromParams(params.urlParams), {
        method: params.method,
        headers: headers,
        body: params.body && JSON.stringify(params.body),
    })
        .then((response) => {
            try {
                const promise = response.json();
                if (promise !== undefined) {
                    promise
                        .then((data) => {
                            if (response.ok) {
                                params.onDataReceived && params.onDataReceived(data);
                            } else {
                                params.handleStatus &&
                                    params.handleStatus({
                                        isOk: response.ok,
                                        status: response.status,
                                        data: data && data,
                                    });
                            }
                        })
                        .catch((err) => {
                            console.log("Error", err);
                            params.handleServerError && params.handleServerError();
                        });
                }
            } catch (err) {
                params.handleServerError && params.handleServerError();
            }
        })
        .catch((err) => {
            console.log(err);
            params.handleServerError && params.handleServerError();
        });
};

export const ServerAPI_GET = (params = DefaultServerAPIParams) => {
    ServerAPI({ ...params, method: "GET" });
};

export const ServerAPI_POST = (params = DefaultServerAPIParams) => {
    ServerAPI({ ...params, method: "POST" });
};

export const ServerAPI_PATCH = (params = DefaultServerAPIParams) => {
    ServerAPI({ ...params, method: "PATCH" });
};

export const ServerAPI_DELETE = (params = DefaultServerAPIParams) => {
    ServerAPI({ ...params, method: "DELETE" });
};
