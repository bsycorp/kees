data "aws_iam_policy_document" "service-c-secrets-policy-data" {
  statement {
    actions = [
      "ssm:GetParameters",
      "ssm:GetParameter"
    ]
    resources = [
      "arn:aws:ssm:${var.aws_region}:${var.aws_account_id}:parameter/app.db",
      "arn:aws:ssm:${var.aws_region}:${var.aws_account_id}:parameter/common.key.v1_private",
      "arn:aws:ssm:${var.aws_region}:${var.aws_account_id}:parameter/common.key.v1_public",
      "arn:aws:ssm:${var.aws_region}:${var.aws_account_id}:parameter/common.thing",
      "arn:aws:ssm:${var.aws_region}:${var.aws_account_id}:parameter/identity-db.url",
      "arn:aws:ssm:${var.aws_region}:${var.aws_account_id}:parameter/service-a.api-key",
      "arn:aws:ssm:${var.aws_region}:${var.aws_account_id}:parameter/service-c-i.api-key",
      "arn:aws:ssm:${var.aws_region}:${var.aws_account_id}:parameter/service-c-i.signing.v1_public",
      "arn:aws:ssm:${var.aws_region}:${var.aws_account_id}:parameter/service-c-n.api-key",
      "arn:aws:ssm:${var.aws_region}:${var.aws_account_id}:parameter/service-c.api-key",
      "arn:aws:ssm:${var.aws_region}:${var.aws_account_id}:parameter/service-n.api-key",
      "arn:aws:ssm:${var.aws_region}:${var.aws_account_id}:parameter/service-p.api-key",
      "arn:aws:ssm:${var.aws_region}:${var.aws_account_id}:parameter/service-t.api-key",
      "arn:aws:ssm:${var.aws_region}:${var.aws_account_id}:parameter/service-t.signing.v1_public"
    ]
  }
}

resource "aws_iam_policy" "service-c-secrets-policy" {
  name   = "service-c-secrets-policy"
  policy = "${data.aws_iam_policy_document.service-c-secrets-policy-data.json}"
}

resource "aws_iam_role_policy_attachment" "service-c-secrets-policy-attachment" {
  role       = "${aws_iam_role.app-role.id}"
  policy_arn = "${aws_iam_policy.service-c-secrets-policy.arn}"
}