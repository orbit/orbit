
export const Actions = {
    REPORT_MESSAGES: "REPORT_MESSAGES",
    REPORT_ADDRESSABLES: "REPORT_ADDRESSABLES"
}

export const reportMessages = (addressableId, messages) => ({
    type: Actions.REPORT_MESSAGES,
    payload: {
        addressableId,
        messages
    }
})

export const reportAddressables = (nodeId, addressables) => ({
    type: Actions.REPORT_ADDRESSABLES,
    payload: {
        nodeId,
        addressables
    }
})
