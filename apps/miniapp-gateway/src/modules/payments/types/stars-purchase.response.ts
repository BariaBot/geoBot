export interface StarPaymentIntentResponse {
  invoiceUrl: string
  payload: {
    productCode: string
    quantity: number
    telegramUserId: number
    intentId: string
  }
  createdAt: string
}
