data "aws_iam_policy_document" "service-c_ddb_secrets_policy_data" {
  statement {
    actions = [
      "dynamodb:GetItem",
      "dynamodb:BatchGetItem"
    ]
    resources = [
      "arn:aws:dynamodb:${var.aws_region}:${var.aws_account_id}:table/${var.kees_table_name}"
    ]

    condition {
      test     = "ForAllValues:StringEquals"
      variable = "dynamodb:LeadingKeys"
      values = [
        "/${var.environment_name}/app.db",
        "/${var.environment_name}/common.key.v1_private",
        "/${var.environment_name}/common.key.v1_public",
        "/${var.environment_name}/common.thing",
        "/${var.environment_name}/identity-db.url",
        "/${var.environment_name}/service-a.api-key",
        "/${var.environment_name}/service-c-i.api-key",
        "/${var.environment_name}/service-c-i.signing.v1_public",
        "/${var.environment_name}/service-c-n.api-key",
        "/${var.environment_name}/service-c.api-key",
        "/${var.environment_name}/service-n.api-key",
        "/${var.environment_name}/service-p.api-key",
        "/${var.environment_name}/service-t.api-key",
        "/${var.environment_name}/service-t.signing.v1_public"
      ]
    }
  }
}

resource "aws_iam_policy" "service-c_ddb_secrets_policy" {
  name   = "${var.environment_name}-service-c-ddb-secrets-policy"
  policy = data.aws_iam_policy_document.service-c_ddb_secrets_policy_data.json
}

resource "aws_iam_role_policy_attachment" "service-c_ddb_secrets_policy_attachment" {
  role       = aws_iam_role.service-c_role.id
  policy_arn = aws_iam_policy.service-c_ddb_secrets_policy.arn
}
